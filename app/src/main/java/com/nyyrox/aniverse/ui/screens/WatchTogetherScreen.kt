package com.nyyrox.aniverse.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.nyyrox.aniverse.data.supabase.WatchTogetherRoom
import com.nyyrox.aniverse.player.CinePlayerView
import com.nyyrox.aniverse.player.IframePlayerView
import com.nyyrox.aniverse.ui.components.OttEpisodeCardItem
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
    var roomCodeInput by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadActiveWatchRooms()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg)
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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x1FFFFFFF))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Watch Together",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = ZenkaiFontFamily
                )
                Text(
                    text = "Sync & stream anime with friends in real time",
                    fontSize = 11.5.sp,
                    color = OttTextMuted
                )
            }
            IconButton(
                onClick = { vm.loadActiveWatchRooms() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = OttPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Join with Room Code Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121520))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x18FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Join a Watch Party", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Enter the 6-digit room code shared by your host", color = OttTextMuted, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = roomCodeInput,
                                onValueChange = { if (it.length <= 8) roomCodeInput = it.trim() },
                                placeholder = { Text("e.g. 849201", color = OttTextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0D0F15),
                                    unfocusedContainerColor = Color(0xFF0D0F15),
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color(0x28FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Button(
                                onClick = {
                                    if (roomCodeInput.isNotBlank()) {
                                        vm.joinWatchTogetherRoom(roomCodeInput) { room ->
                                            if (room == null) {
                                                Toast.makeText(context, "Room not found or invalid code", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = roomCodeInput.isNotBlank() && !vm.isWatchRoomLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                if (vm.isWatchRoomLoading) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Join", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Create Watch Party Banner Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161924))
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Host Your Own Party", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text("Invite friends and control episode & servers together", color = OttTextSecondary, fontSize = 11.5.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = OttPillShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Host", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Active Public Rooms Title
            item {
                Text(
                    text = "Active Public Parties",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ZenkaiFontFamily
                )
            }

            if (vm.isWatchRoomLoading && vm.activeWatchRoomsList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OttPrimary)
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
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = OttTextMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No active public parties right now", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Create a room or join with a code from a friend!", color = OttTextMuted, fontSize = 11.sp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OttSurface)
            .border(1.dp, OttSurfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onJoin() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = room.animeCover,
            contentDescription = room.animeTitle,
            modifier = Modifier
                .size(width = 60.dp, height = 80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = room.animeTitle,
                color = OttTextSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ep ${room.episode}",
                    color = OttPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = OttTextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${room.members.size.coerceAtLeast(1)}",
                    color = OttTextMuted,
                    fontSize = 11.5.sp
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onJoin,
            colors = ButtonDefaults.buttonColors(containerColor = OttPrimary),
            shape = OttPillShape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text("Join", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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
                .background(Color(0xFF12151F))
                .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column {
                Text("Create Watch Party", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
                        focusedContainerColor = Color(0xFF090B10),
                        unfocusedContainerColor = Color(0xFF090B10),
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x28FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
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
                        focusedContainerColor = Color(0xFF090B10),
                        unfocusedContainerColor = Color(0xFF090B10),
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x28FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
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
                                            if (selectedAnime?.id == a.id) Color.White else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                                Text(a.title, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            Text(anime.displayTitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        Text("Cancel", color = Color.White)
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (vm.isCreatingWatchRoom) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
            .background(Color(0xFF08090D))
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
        }

        // Small Sync Status Banner (Under Player)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F121C))
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

        // Two-Row Tabs (Chat & Playback/Details)
        val tabs = if (isHost) listOf("💬 Room Chat", "🎬 Playback & Servers (Host)") else listOf("💬 Room Chat", "ℹ️ Room Details & Status")

        TabRow(
            selectedTabIndex = activeTab.coerceIn(0, tabs.size - 1),
            containerColor = Color(0xFF0F121C),
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
            .background(Color(0xFF090B10))
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
            .background(Color(0xFF090B10)),
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
            .background(Color(0xFF090B10)),
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
