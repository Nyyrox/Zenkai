package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.nyyrox.aniverse.util.DateTimeUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.giphy.GifItem
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.data.supabase.CloudProfile
import com.nyyrox.aniverse.data.supabase.CommunityMessage
import com.nyyrox.aniverse.ui.components.decorations.AvatarDecorationRenderer
import com.nyyrox.aniverse.ui.components.decorations.RoleBadgeRow
import com.nyyrox.aniverse.ui.components.decorations.TextEffectRenderer
import com.nyyrox.aniverse.ui.theme.*
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun isUserOnline(lastSeenAt: String?): Boolean {
    return com.nyyrox.aniverse.util.DateTimeUtils.isUserOnline(lastSeenAt)
}

fun formatLastSeen(lastSeenAt: String?): String {
    return com.nyyrox.aniverse.util.DateTimeUtils.formatLastSeen(lastSeenAt)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectChatScreen(
    partnerUserId: String,
    vm: MainViewModel,
    onBack: () -> Unit,
    onUserProfileClick: (String) -> Unit = {},
    onAnimeClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var partnerProfile by remember { mutableStateOf<CloudProfile?>(vm.activeDmPartnerProfile) }
    var inputText by remember { mutableStateOf("") }
    var isSpoilerMessage by remember { mutableStateOf(false) }
    var selectedRecAnime by remember { mutableStateOf<Anime?>(null) }
    var showAnimePickerForRec by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }
    var selectedGifItem by remember { mutableStateOf<GifItem?>(null) }
    var showImageUrlDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var tempImageUrlInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<CommunityMessage?>(null) }
    var messageForActions by remember { mutableStateOf<CommunityMessage?>(null) }
    var messageToDelete by remember { mutableStateOf<CommunityMessage?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val messages = vm.directChatMessages
    val myUserId = (vm.authState as? AuthState.LoggedIn)?.userId ?: vm.userProfile.id

    // Fetch partner profile if null
    LaunchedEffect(partnerUserId) {
        if (partnerProfile == null || partnerProfile?.id != partnerUserId) {
            vm.fetchPublicProfile(partnerUserId) { p ->
                partnerProfile = p
            }
        }
    }

    // Periodic poll for DM conversation updates every 5 seconds
    LaunchedEffect(partnerUserId) {
        vm.loadDirectChatMessages(partnerUserId)
        while (true) {
            delay(5000)
            vm.loadDirectChatMessages(partnerUserId)
        }
    }

    // Scroll to bottom on new messages
    var initialScrollDone by remember(partnerUserId) { mutableStateOf(false) }
    LaunchedEffect(messages.size, partnerUserId) {
        if (messages.isNotEmpty()) {
            val isAtBottom = !listState.canScrollForward || !initialScrollDone
            if (isAtBottom) {
                listState.animateScrollToItem(messages.size - 1)
                initialScrollDone = true
            }
        }
    }

    val partnerName = partnerProfile?.displayName?.ifBlank { null } ?: partnerProfile?.username ?: "Otaku"
    val isOnline = isUserOnline(partnerProfile?.lastSeenAt) && partnerProfile?.isOnline != false
    val activityText = if (isOnline) partnerProfile?.currentActivity?.ifBlank { null } else null
    val lastSeenText = if (isOnline) "Online" else formatLastSeen(partnerProfile?.lastSeenAt)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OttPanelHigh)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OttTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Partner Avatar with 🟢 Glowing Online Ring
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onUserProfileClick(partnerUserId) },
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarDecorationRenderer(
                            decorationId = partnerProfile?.avatarDecoration,
                            frameId = partnerProfile?.avatarFrame,
                            size = 38.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2330))
                                    .border(
                                        1.5.dp,
                                        if (isOnline) Color(0xFF10B981) else OttInk(0.20f),
                                        CircleShape
                                    )
                            ) {
                                if (!partnerProfile?.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = partnerProfile!!.avatarUrl,
                                        contentDescription = partnerName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(OttPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = partnerName.take(1).uppercase(),
                                            color = OttTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Glowing Online status dot
                        if (isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                                    .border(1.5.dp, OttPanelHigh, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Partner Name & Live Activity Status
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUserProfileClick(partnerUserId) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextEffectRenderer(
                                text = partnerName,
                                effectId = partnerProfile?.textEffect,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (partnerProfile?.isAdmin == true || partnerProfile?.role == "ADMIN") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE11D48))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "STAFF",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OttTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Activity Status Badge
                        if (!activityText.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val actType = partnerProfile?.currentActivityType ?: "ANIME"
                                val icon = when (actType) {
                                    "ANIME" -> "🎬"
                                    "MANGA" -> "📖"
                                    "NOVEL" -> "📜"
                                    "COMMUNITY" -> "💬"
                                    else -> "✨"
                                }
                                Text(
                                    text = "$icon $activityText",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Online",
                                        fontSize = 11.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = lastSeenText,
                                        fontSize = 11.sp,
                                        color = OttTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Message List Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (vm.isDirectChatLoading && messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFE11D48),
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else if (messages.isEmpty()) {
                    // Empty DM State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color(0x15E11D48))
                                .border(1.dp, Color(0x35E11D48), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Direct Chat with $partnerName",
                            color = OttTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Send your first message, share anime recommendations, GIFs, or discuss your favorite episodes.",
                            color = OttTextMuted,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                            val prevMsg = if (index > 0) messages[index - 1] else null
                            val showDateHeader = prevMsg == null ||
                                    DateTimeUtils.getDayKey(msg.createdAt) != DateTimeUtils.getDayKey(prevMsg.createdAt)

                            if (showDateHeader) {
                                ChatDateHeader(
                                    dateText = DateTimeUtils.formatMessageDateHeader(msg.createdAt),
                                    modifier = Modifier.padding(top = if (index > 0) 8.dp else 0.dp, bottom = 4.dp)
                                )
                            }

                            val isCurrentUser = (myUserId != null && msg.userId == myUserId)
                            DirectChatMessageItem(
                                message = msg,
                                isCurrentUser = isCurrentUser,
                                onAnimeClick = onAnimeClick,
                                onMessageHold = { messageForActions = it }
                            )
                        }
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                color = OttSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .border(width = 0.5.dp, color = OttInk(0.13f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Replying to banner
                    if (replyingToMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2433))
                                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Replying to: \"${replyingToMessage!!.message.take(60)}\"",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { replyingToMessage = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Selected Anime Attachment Banner
                    if (selectedRecAnime != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2433))
                                .border(1.dp, Color(0x33E11D48), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = selectedRecAnime!!.cover,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attached: ${selectedRecAnime!!.title}",
                                    color = OttTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { selectedRecAnime = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Anime",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Selected GIF Attachment Banner
                    if (selectedGifItem != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2433))
                                .border(1.dp, Color(0x33E11D48), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = selectedGifItem!!.previewUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attached GIF: ${selectedGifItem!!.title.ifBlank { "Anime GIF" }}",
                                    color = OttTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { selectedGifItem = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove GIF",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Selected Image Attachment Banner
                    if (selectedImageUrl != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2433))
                                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = selectedImageUrl!!,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attached Image URL",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { selectedImageUrl = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Input Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Anime Recommendation Picker
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedRecAnime != null) Color(0x33E11D48) else OttInk(0.07f))
                                .border(
                                    1.dp,
                                    if (selectedRecAnime != null) Color(0xFFE11D48) else OttInk(0.16f),
                                    CircleShape
                                )
                                .clickable { showAnimePickerForRec = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Anime Rec",
                                tint = if (selectedRecAnime != null) Color(0xFFE11D48) else OttTextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(5.dp))

                        // Spoiler Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSpoilerMessage) Color(0x33F59E0B) else OttInk(0.07f))
                                .border(
                                    1.dp,
                                    if (isSpoilerMessage) Color(0xFFF59E0B) else OttInk(0.16f),
                                    CircleShape
                                )
                                .clickable { isSpoilerMessage = !isSpoilerMessage },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSpoilerMessage) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Spoiler toggle",
                                tint = if (isSpoilerMessage) Color(0xFFF59E0B) else OttTextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(5.dp))

                        // GIF Picker
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedGifItem != null) Color(0x33E11D48) else OttInk(0.07f))
                                .border(
                                    1.dp,
                                    if (selectedGifItem != null) Color(0xFFE11D48) else OttInk(0.16f),
                                    CircleShape
                                )
                                .clickable { showGifPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "GIF Picker",
                                tint = if (selectedGifItem != null) Color(0xFFE11D48) else OttTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(5.dp))

                        // Image Attachment
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedImageUrl != null) Color(0x3338BDF8) else OttInk(0.07f))
                                .border(
                                    1.dp,
                                    if (selectedImageUrl != null) Color(0xFF38BDF8) else OttInk(0.16f),
                                    CircleShape
                                )
                                .clickable { showImageUrlDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Attach Image",
                                tint = if (selectedImageUrl != null) Color(0xFF38BDF8) else OttTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Message $partnerName...",
                                    color = OttTextMuted,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = OttPanelHigh,
                                unfocusedContainerColor = OttPanelHigh,
                                focusedBorderColor = if (isSpoilerMessage) Color(0xFFF59E0B) else Color(0xFFE11D48),
                                unfocusedBorderColor = OttInk(0.19f),
                                focusedTextColor = OttTextPrimary,
                                unfocusedTextColor = OttTextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if ((inputText.isNotBlank() || selectedRecAnime != null || selectedGifItem != null || selectedImageUrl != null) && !vm.isSendingDirectMessage) {
                                        val textToSend = when {
                                            selectedRecAnime != null -> {
                                                val a = selectedRecAnime!!
                                                val scoreStr = if ((a.score ?: 0) > 0) a.score.toString() else ""
                                                val encoded = "[ANIME_REC|${a.id}|${a.title}|${a.cover}|$scoreStr|${a.format}] $inputText".trim()
                                                selectedRecAnime = null
                                                encoded
                                            }
                                            selectedGifItem != null -> {
                                                val g = selectedGifItem!!
                                                val encoded = "[GIF|${g.url}|${g.title}] $inputText".trim()
                                                selectedGifItem = null
                                                encoded
                                            }
                                            selectedImageUrl != null -> {
                                                val url = selectedImageUrl!!
                                                val encoded = if (isSpoilerMessage) "[SPOILER] [IMG|$url] $inputText".trim() else "[IMG|$url] $inputText".trim()
                                                selectedImageUrl = null
                                                encoded
                                            }
                                            isSpoilerMessage -> "[SPOILER] $inputText"
                                            else -> inputText
                                        }
                                        val currentReply = replyingToMessage
                                        inputText = ""
                                        isSpoilerMessage = false
                                        replyingToMessage = null
                                        vm.sendDirectMessage(
                                            partnerUserId = partnerUserId,
                                            text = textToSend,
                                            replyToId = currentReply?.id,
                                            replyToUsername = currentReply?.displayName?.ifBlank { null } ?: currentReply?.username ?: "Otaku",
                                            replyToText = currentReply?.message?.take(100)
                                        )
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                if ((inputText.isNotBlank() || selectedRecAnime != null || selectedGifItem != null || selectedImageUrl != null) && !vm.isSendingDirectMessage) {
                                    val textToSend = when {
                                        selectedRecAnime != null -> {
                                            val a = selectedRecAnime!!
                                            val scoreStr = if ((a.score ?: 0) > 0) a.score.toString() else ""
                                            val encoded = "[ANIME_REC|${a.id}|${a.title}|${a.cover}|$scoreStr|${a.format}] $inputText".trim()
                                            selectedRecAnime = null
                                            encoded
                                        }
                                        selectedGifItem != null -> {
                                            val g = selectedGifItem!!
                                            val encoded = "[GIF|${g.url}|${g.title}] $inputText".trim()
                                            selectedGifItem = null
                                            encoded
                                        }
                                        selectedImageUrl != null -> {
                                            val url = selectedImageUrl!!
                                            val encoded = if (isSpoilerMessage) "[SPOILER] [IMG|$url] $inputText".trim() else "[IMG|$url] $inputText".trim()
                                            selectedImageUrl = null
                                            encoded
                                        }
                                        isSpoilerMessage -> "[SPOILER] $inputText"
                                        else -> inputText
                                    }
                                    val currentReply = replyingToMessage
                                    inputText = ""
                                    isSpoilerMessage = false
                                    replyingToMessage = null
                                    vm.sendDirectMessage(
                                        partnerUserId = partnerUserId,
                                        text = textToSend,
                                        replyToId = currentReply?.id,
                                        replyToUsername = currentReply?.displayName?.ifBlank { null } ?: currentReply?.username ?: "Otaku",
                                        replyToText = currentReply?.message?.take(100)
                                    )
                                }
                            },
                            enabled = (inputText.isNotBlank() || selectedRecAnime != null || selectedGifItem != null || selectedImageUrl != null) && !vm.isSendingDirectMessage,
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpoilerMessage) Color(0xFFF59E0B) else Color(0xFFE11D48),
                                disabledContainerColor = OttInk(0.13f)
                            )
                        ) {
                            if (vm.isSendingDirectMessage) {
                                CircularProgressIndicator(
                                    color = OttTextPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = OttTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // AniList Anime Picker Sheet for Recommendations
        if (showAnimePickerForRec) {
            DirectChatAnimePickerModal(
                onDismiss = { showAnimePickerForRec = false },
                onAnimeSelected = { anime ->
                    selectedRecAnime = anime
                    showAnimePickerForRec = false
                },
                vm = vm
            )
        }

        // Image Attachment Dialog
        if (showImageUrlDialog) {
            AlertDialog(
                onDismissRequest = { showImageUrlDialog = false },
                containerColor = OttPanelHigh,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Attach Image URL", color = OttTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Paste a direct link to an image (JPEG, PNG, WebP, GIF):",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = tempImageUrlInput,
                            onValueChange = { tempImageUrlInput = it },
                            placeholder = { Text("https://example.com/image.jpg", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OttTextPrimary,
                                unfocusedTextColor = OttTextPrimary,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = OttInk(0.19f)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempImageUrlInput.isNotBlank()) {
                                selectedImageUrl = tempImageUrlInput.trim()
                                tempImageUrlInput = ""
                            }
                            showImageUrlDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        enabled = tempImageUrlInput.isNotBlank()
                    ) {
                        Text("Attach", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showImageUrlDialog = false
                            tempImageUrlInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OttInk(0.13f))
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        // GIF Picker Bottom Sheet
        if (showGifPicker) {
            GifPickerModal(
                onDismiss = { showGifPicker = false },
                onSelectGif = { gif ->
                    selectedGifItem = gif
                    showGifPicker = false
                }
            )
        }

        // Message Actions Sheet
        if (messageForActions != null) {
            val targetMsg = messageForActions!!
            val isAuthor = myUserId != null && myUserId == targetMsg.userId

            AlertDialog(
                onDismissRequest = { messageForActions = null },
                containerColor = OttPanelHigh,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Message Actions",
                        color = OttTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Reply
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161F30))
                                .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(12.dp))
                                .clickable {
                                    val m = targetMsg
                                    messageForActions = null
                                    replyingToMessage = m
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Reply",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Reply to Message",
                                color = OttTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Delete
                        if (isAuthor) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x22EF4444))
                                    .border(1.dp, Color(0x55EF4444), RoundedCornerShape(12.dp))
                                    .clickable {
                                        val m = targetMsg
                                        messageForActions = null
                                        messageToDelete = m
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Message",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Delete Message",
                                    color = Color(0xFFEF4444),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(
                        onClick = { messageForActions = null },
                        colors = ButtonDefaults.buttonColors(containerColor = OttInk(0.13f))
                    ) {
                        Text("Close", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (messageToDelete != null) {
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                containerColor = OttPanelHigh,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Delete Message", color = OttTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Text(
                        "Are you sure you want to delete this message? This action cannot be undone.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val m = messageToDelete ?: return@Button
                            messageToDelete = null
                            vm.deleteDirectMessage(m.id, partnerUserId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Delete", color = OttTextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { messageToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = OttInk(0.13f))
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectChatMessageItem(
    message: CommunityMessage,
    isCurrentUser: Boolean,
    onAnimeClick: (Int) -> Unit = {},
    onMessageHold: (CommunityMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 3.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 16.dp)
    }

    val bubbleBg = if (isCurrentUser) {
        Brush.verticalGradient(listOf(Color(0xFF2E0D1F), OttPanelHigh))
    } else {
        Brush.verticalGradient(listOf(OttPanelHigh, OttPanelHigh))
    }

    val bubbleBorder = if (isCurrentUser) {
        Color(0x55E11D48)
    } else {
        OttInk(0.13f)
    }

    val rawMsg = message.message
    val isSpoiler = rawMsg.contains("[SPOILER]") || rawMsg.startsWith("||")
    var isSpoilerRevealed by remember(message.id) { mutableStateOf(!isSpoiler) }
    val animatedBlur by animateDpAsState(
        targetValue = if (isSpoiler && !isSpoilerRevealed) 16.dp else 0.dp,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "dm_spoiler_blur"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleBg)
                    .border(1.dp, bubbleBorder, bubbleShape)
                    .combinedClickable(
                        onClick = {
                            if (isSpoiler) isSpoilerRevealed = !isSpoilerRevealed
                        },
                        onLongClick = {
                            onMessageHold(message)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val cleanText = rawMsg.replace("[SPOILER]", "").trim()
                val animeRec = remember(cleanText) { parseAnimeRecommendation(cleanText) }
                val gifData = remember(cleanText) { parseGifComment(cleanText) }
                val imgData = remember(cleanText) { parseImageComment(cleanText) }

                Column(
                    modifier = Modifier
                        .then(if (animeRec != null || gifData != null || imgData != null) Modifier.fillMaxWidth() else Modifier)
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (isSpoiler) Modifier.blur(animatedBlur) else Modifier)
                ) {
                    // Discord-style quote reference
                    if (!message.replyToUsername.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2238BDF8))
                                .border(0.5.dp, Color(0x4438BDF8), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "@${message.replyToUsername}",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!message.replyToText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = message.replyToText.replace("[SPOILER]", "").trim(),
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    when {
                        animeRec != null -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OttPanelHigh)
                                        .border(1.dp, OttInk(0.20f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (animeRec.animeId > 0) onAnimeClick(animeRec.animeId)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = animeRec.cover,
                                        contentDescription = animeRec.title,
                                        modifier = Modifier
                                            .width(52.dp)
                                            .height(74.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = animeRec.title,
                                            color = OttTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Tap to view anime",
                                            color = OttPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (animeRec.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "\"${animeRec.comment}\"",
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        gifData != null -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = gifData.url,
                                    contentDescription = gifData.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OttPanelHigh),
                                    contentScale = ContentScale.Fit
                                )
                                if (gifData.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = gifData.comment,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 13.5.sp,
                                        lineHeight = 18.5.sp
                                    )
                                }
                            }
                        }
                        imgData != null -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = imgData.url,
                                    contentDescription = "Attached Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 260.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OttPanelHigh),
                                    contentScale = ContentScale.Fit
                                )
                                if (imgData.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = imgData.comment,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 13.5.sp,
                                        lineHeight = 18.5.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = cleanText,
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.5.sp,
                                lineHeight = 18.5.sp
                            )
                        }
                    }
                }
            }

            // Timestamp / Status under bubble
            if (!message.createdAt.isNullOrBlank()) {
                val formattedTime = DateTimeUtils.formatMessageTime(message.createdAt)
                if (formattedTime.isNotBlank()) {
                    Text(
                        text = formattedTime,
                        fontSize = 9.5.sp,
                        color = OttTextMuted.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatAnimePickerModal(
    onDismiss: () -> Unit,
    onAnimeSelected: (Anime) -> Unit,
    vm: MainViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            isSearching = true
            delay(400)
            val results = vm.searchAniListAnime(searchQuery)
            searchResults = results
            isSearching = false
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OttPanelHigh,
        dragHandle = { BottomSheetDefaults.DragHandle(color = OttInk(0.25f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Recommend,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recommend an Anime", color = OttTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search anime by title...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OttTextPrimary,
                    unfocusedTextColor = OttTextPrimary,
                    focusedBorderColor = Color(0xFFF59E0B),
                    unfocusedBorderColor = OttInk(0.19f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.trim().length < 2) "Type at least 2 letters to search" else "No anime found",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { anime ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C2230))
                                .clickable { onAnimeSelected(anime) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = anime.cover,
                                contentDescription = anime.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 44.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.title ?: "Unknown",
                                    color = OttTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${anime.format ?: "TV"} • ⭐ ${anime.score ?: "N/A"}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
