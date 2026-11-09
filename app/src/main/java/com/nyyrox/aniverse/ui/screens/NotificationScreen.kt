package com.nyyrox.aniverse.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.supabase.AppNotification
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.ui.components.decorations.AvatarDecorationRenderer
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun handleNotificationUrl(
    context: Context,
    rawUrl: String,
    onNavigate: (String) -> Unit,
    onNavigateToCommunity: (String) -> Unit,
    onNavigateToDirectChat: (String) -> Unit,
    onClaimReward: (() -> Unit)? = null
) {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return

    // 1. Reward action
    if (trimmed == "zenkai://rewards/welcome" || trimmed.startsWith("zenkai://rewards")) {
        onClaimReward?.invoke()
        onNavigate("profile")
        return
    }

    // 2. In-app custom scheme (zenkai:// or aniverse://)
    if (trimmed.startsWith("zenkai://") || trimmed.startsWith("aniverse://")) {
        val uri = Uri.parse(trimmed)
        val host = uri.host.orEmpty()
        val path = uri.path.orEmpty().trimStart('/')
        when (host) {
            "community" -> onNavigateToCommunity(path.ifBlank { "general" })
            "direct_chat", "dm" -> {
                if (path.isNotBlank()) onNavigateToDirectChat(path)
                else onNavigateToCommunity("general")
            }
            "anime", "detail" -> if (path.isNotBlank()) onNavigate("detail:$path")
            "manga" -> if (path.isNotBlank()) onNavigate("manga_detail:$path")
            "novel" -> if (path.isNotBlank()) onNavigate("novel_detail:$path")
            "notifications" -> onNavigate("notifications")
            "profile" -> onNavigate("profile")
            else -> onNavigate("profile")
        }
        return
    }

    // 3. Raw route names
    when {
        trimmed.startsWith("detail:") || trimmed.startsWith("manga_detail:") ||
        trimmed.startsWith("novel_detail:") || trimmed.startsWith("direct_chat:") -> {
            onNavigate(trimmed)
            return
        }
        trimmed == "community" -> {
            onNavigateToCommunity("general")
            return
        }
        trimmed == "profile" -> {
            onNavigate("profile")
            return
        }
    }

    // 4. Web universal deep links
    if (trimmed.contains("aniverse-kt.vercel.app") || trimmed.contains("aniverse.app")) {
        val uri = Uri.parse(trimmed)
        val path = uri.path.orEmpty()
        when {
            path.startsWith("/anime/") -> { onNavigate("detail:${path.removePrefix("/anime/")}"); return }
            path.startsWith("/manga/") -> { onNavigate("manga_detail:${path.removePrefix("/manga/")}"); return }
            path.startsWith("/novel/") -> { onNavigate("novel_detail:${path.removePrefix("/novel/")}"); return }
            path.startsWith("/community") -> { onNavigateToCommunity("general"); return }
        }
    }

    // 5. External Web URLs (e.g. https://... or domain.com)
    try {
        val finalUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.contains("://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open link: $trimmed", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NotificationScreen(
    vm: MainViewModel,
    initialNotificationId: String? = null,
    onBack: () -> Unit,
    onNavigateToCommunity: (String) -> Unit = {},
    onNavigateToDirectChat: (String) -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLoggedIn = vm.authState is AuthState.LoggedIn

    var selectedNotificationForDetails by remember { mutableStateOf<AppNotification?>(null) }

    LaunchedEffect(Unit) {
        vm.loadNotifications()
    }

    val allNotifs = vm.notifications

    LaunchedEffect(initialNotificationId, allNotifs) {
        if (!initialNotificationId.isNullOrBlank() && selectedNotificationForDetails == null) {
            val match = allNotifs.firstOrNull { it.id == initialNotificationId }
            if (match != null) {
                if (!match.isRead) vm.markNotificationAsRead(match.id)
                selectedNotificationForDetails = match
            }
        }
    }

    val unreadCount = vm.unreadNotificationsCount

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Header Bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NOTIFICATIONS",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFE11D48))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unreadCount new",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "System updates & community announcements",
                        color = OttTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isLoggedIn && unreadCount > 0) {
                        IconButton(
                            onClick = { vm.markAllNotificationsAsRead() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all read",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { vm.loadNotifications() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Divider below header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x18FFFFFF))
            )

            // ── Single Unified Feed Content ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (allNotifs.isEmpty() && !vm.isNotificationsLoading) {
                    if (!isLoggedIn) {
                        GuestNotificationPrompt(onNavigateToLogin = onNavigateToLogin)
                    } else {
                        EmptyNotificationState(
                            icon = Icons.Default.Campaign,
                            title = "No Notifications",
                            subtitle = "You're all caught up! System broadcasts and updates will appear here."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allNotifs, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                onCardClick = {
                                    if (!notif.isRead) vm.markNotificationAsRead(notif.id)
                                    selectedNotificationForDetails = notif
                                },
                                onDelete = { vm.deleteNotification(notif.id) },
                                onClaimReward = { vm.claimNotificationReward(notif) },
                                onOpenUrl = { url ->
                                    handleNotificationUrl(
                                        context = context,
                                        rawUrl = url,
                                        onNavigate = onNavigate,
                                        onNavigateToCommunity = onNavigateToCommunity,
                                        onNavigateToDirectChat = onNavigateToDirectChat,
                                        onClaimReward = { vm.claimNotificationReward(notif) }
                                    )
                                }
                            )
                        }
                    }
                }

                if (vm.isNotificationsLoading && allNotifs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = OttPrimary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }
        }

        selectedNotificationForDetails?.let { detailNotif ->
            NotificationDetailsDialog(
                notification = detailNotif,
                onDismiss = { selectedNotificationForDetails = null },
                onClaimReward = {
                    vm.claimNotificationReward(detailNotif)
                    selectedNotificationForDetails = detailNotif.copy(isClaimed = true, isRead = true)
                },
                onNavigateAction = { rawUrl ->
                    selectedNotificationForDetails = null
                    handleNotificationUrl(
                        context = context,
                        rawUrl = rawUrl,
                        onNavigate = onNavigate,
                        onNavigateToCommunity = onNavigateToCommunity,
                        onNavigateToDirectChat = onNavigateToDirectChat,
                        onClaimReward = { vm.claimNotificationReward(detailNotif) }
                    )
                },
                onNavigateToDirectChat = { partnerId ->
                    selectedNotificationForDetails = null
                    onNavigateToDirectChat(partnerId)
                },
                onNavigateToCommunity = { channel ->
                    selectedNotificationForDetails = null
                    onNavigateToCommunity(channel)
                }
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: AppNotification,
    onCardClick: () -> Unit,
    onDelete: () -> Unit,
    onClaimReward: () -> Unit = {},
    onOpenUrl: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRead = notification.isRead
    val timeAgo = remember(notification.createdAt) {
        formatNotificationTimestamp(notification.createdAt)
    }

    val typeBadgeColor = when (notification.type) {
        "mention", "reply" -> Color(0xFF38BDF8)
        "global", "announcement", "broadcast" -> Color(0xFFF59E0B)
        "admin" -> Color(0xFFE11D48)
        "system" -> Color(0xFF10B981)
        "reward" -> Color(0xFF10B981)
        "alert" -> Color(0xFFEF4444)
        "update" -> Color(0xFF8B5CF6)
        else -> Color(0xFF818CF8)
    }

    val cardBg = if (isRead) {
        Color(0xFF11141D)
    } else {
        Color(0xFF161B28)
    }

    val cardBorder = if (isRead) {
        Color(0x18FFFFFF)
    } else {
        typeBadgeColor.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onCardClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Sender Avatar
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarDecorationRenderer(
                    frameId = notification.senderFrame,
                    decorationId = notification.senderDecoration,
                    size = 42.dp
                ) {
                    val avatarModel: Any? = when {
                        !notification.senderAvatar.isNullOrBlank() && !notification.senderAvatar.contains("welcome_nano_banana") -> notification.senderAvatar
                        notification.senderAvatar?.contains("welcome_nano_banana") == true || notification.id.startsWith("welcome_") -> com.nyyrox.aniverse.R.drawable.welcome_nano_banana
                        else -> notification.senderAvatar
                    }
                    if (avatarModel != null) {
                        AsyncImage(
                            model = avatarModel,
                            contentDescription = "Sender Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF2E3440), Color(0xFF191D24)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (notification.type) {
                                    "global", "announcement", "broadcast" -> Icons.Default.Campaign
                                    "reward" -> Icons.Default.CardGiftcard
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.senderName ?: "ZENKAI System",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (notification.type != "dm") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(typeBadgeColor.copy(alpha = 0.18f))
                                    .border(0.5.dp, typeBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = notification.type.uppercase(),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = typeBadgeColor
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeAgo,
                            color = Color(0xFF64748B),
                            fontSize = 10.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Title
                if (notification.title.isNotBlank()) {
                    Text(
                        text = notification.title,
                        color = if (!isRead) Color(0xFFF1F5F9) else Color(0xFFCBD5E1),
                        fontSize = 12.5.sp,
                        fontWeight = if (!isRead) FontWeight.Bold else FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Message snippet
                Text(
                    text = notification.message,
                    color = if (!isRead) Color(0xFFE2E8F0) else OttTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                // Optional Attached Image Preview Banner
                if (!notification.imageUrl.isNullOrBlank() || notification.id.startsWith("welcome_")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val imageModel: Any = when {
                        !notification.imageUrl.isNullOrBlank() && !notification.imageUrl.contains("welcome_nano_banana") -> notification.imageUrl
                        notification.imageUrl?.contains("welcome_nano_banana") == true || notification.id.startsWith("welcome_") -> com.nyyrox.aniverse.R.drawable.welcome_nano_banana
                        else -> ""
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Notification Media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F121C))
                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Optional Reward / Action Buttons
                if (notification.type == "reward" || !notification.actionUrl.isNullOrBlank() || !notification.actionLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (notification.isClaimed) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1810B981))
                                .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Reward Claimed ✓",
                                color = Color(0xFF6EE7B7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (notification.type == "reward") {
                                Button(
                                    onClick = onClaimReward,
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = notification.actionLabel ?: "Claim Reward (+100 EXP)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            if (!notification.actionUrl.isNullOrBlank()) {
                                Button(
                                    onClick = {
                                        if (onOpenUrl != null) {
                                            onOpenUrl(notification.actionUrl)
                                        } else {
                                            try {
                                                val uri = Uri.parse(notification.actionUrl)
                                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color(0xFF090B10),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = notification.actionLabel?.takeIf { notification.type != "reward" } ?: "Open Link",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF090B10)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!notification.channel.isNullOrBlank()) {
                    val isDm = notification.type == "dm" || notification.channel.startsWith("dm_")
                    if (!isDm) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "#${notification.channel} • Tap to view chat",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (!isRead) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(typeBadgeColor)
                )
            }
        }
    }
}

@Composable
fun NotificationDetailsDialog(
    notification: AppNotification,
    onDismiss: () -> Unit,
    onClaimReward: () -> Unit,
    onNavigateAction: (String) -> Unit,
    onNavigateToDirectChat: (String) -> Unit,
    onNavigateToCommunity: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val isClaimed = notification.isClaimed
    val timeAgo = remember(notification.createdAt) {
        formatNotificationTimestamp(notification.createdAt)
    }

    val typeBadgeColor = when (notification.type) {
        "mention", "reply" -> Color(0xFF38BDF8)
        "global", "announcement", "broadcast" -> Color(0xFFF59E0B)
        "admin" -> Color(0xFFE11D48)
        "system" -> Color(0xFF10B981)
        "reward" -> Color(0xFF10B981)
        "alert" -> Color(0xFFEF4444)
        "update" -> Color(0xFF8B5CF6)
        else -> Color(0xFF818CF8)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(onClick = onDismiss)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF131722))
                    .border(1.2.dp, typeBadgeColor.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .clickable(enabled = false) {}
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top header: Category badge, timestamp & Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(typeBadgeColor.copy(alpha = 0.2f))
                                    .border(1.dp, typeBadgeColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = notification.type.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = typeBadgeColor
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeAgo,
                                fontSize = 11.sp,
                                color = OttTextMuted
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Sender Profile Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AvatarDecorationRenderer(
                                frameId = notification.senderFrame,
                                decorationId = notification.senderDecoration,
                                size = 44.dp
                            ) {
                                val avatarModel: Any? = when {
                                    !notification.senderAvatar.isNullOrBlank() && !notification.senderAvatar.contains("welcome_nano_banana") -> notification.senderAvatar
                                    notification.senderAvatar?.contains("welcome_nano_banana") == true || notification.id.startsWith("welcome_") -> com.nyyrox.aniverse.R.drawable.welcome_nano_banana
                                    else -> notification.senderAvatar
                                }
                                if (avatarModel != null) {
                                    AsyncImage(
                                        model = avatarModel,
                                        contentDescription = "Sender Avatar",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(Color(0xFF2E3440), Color(0xFF191D24)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (notification.type) {
                                                "global", "announcement", "broadcast" -> Icons.Default.Campaign
                                                "reward" -> Icons.Default.CardGiftcard
                                                else -> Icons.Default.Person
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = notification.senderName ?: "ZENKAI System",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (notification.userId == null) "Global Broadcast • Server Verified" else "Direct Notification",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Notification Title
                    if (notification.title.isNotBlank()) {
                        Text(
                            text = notification.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Notification Message (Full, formatted)
                    Text(
                        text = notification.message,
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 19.sp
                    )

                    // Optional Attached Media Banner
                    val imageModel: Any? = when {
                        !notification.imageUrl.isNullOrBlank() && !notification.imageUrl.contains("welcome_nano_banana") -> notification.imageUrl
                        notification.imageUrl?.contains("welcome_nano_banana") == true || notification.id.startsWith("welcome_") -> com.nyyrox.aniverse.R.drawable.welcome_nano_banana
                        else -> null
                    }
                    if (imageModel != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E2333))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Notification Media Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Action / Reward Buttons
                    val isReward = notification.type == "reward" || notification.actionUrl?.startsWith("zenkai://rewards") == true
                    val hasActionUrl = !notification.actionUrl.isNullOrBlank()
                    val isDm = notification.type == "dm" || notification.channel?.startsWith("dm_") == true

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isReward) {
                            if (!isClaimed) {
                                Button(
                                    onClick = onClaimReward,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = notification.actionLabel?.ifBlank { "Claim +100 Starter EXP" } ?: "Claim +100 Starter EXP",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x2210B981))
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Reward Claimed (+100 EXP Added)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6EE7B7)
                                        )
                                    }
                                }
                            }
                        }

                        if (isDm) {
                            val partnerId = notification.targetId?.takeIf { it.isNotBlank() }
                                ?: notification.senderId?.takeIf { it.isNotBlank() }
                                ?: notification.actionUrl?.removePrefix("aniverse://dm/")?.removePrefix("direct_chat:")
                                ?: ""
                            Button(
                                onClick = {
                                    if (partnerId.isNotBlank()) onNavigateToDirectChat(partnerId)
                                    else if (!notification.channel.isNullOrBlank()) onNavigateToCommunity(notification.channel)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Direct Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (hasActionUrl && !isReward) {
                            Button(
                                onClick = { onNavigateAction(notification.actionUrl!!) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = notification.actionLabel?.ifBlank { "View Details" } ?: "View Details",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x1438BDF8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = OttTextMuted,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GuestNotificationPrompt(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x14E11D48)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Color(0xFFE11D48),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Sign in for Personal Notifications",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Sign in to receive community mentions, replies, and direct alerts sent to your profile.",
            color = OttTextMuted,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToLogin,
            colors = ButtonDefaults.buttonColors(containerColor = OttPrimary),
            shape = OttPillShape
        ) {
            Text("Sign In Now", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatNotificationTimestamp(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return "Just now"
    return try {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val clean = if (rawDate.length > 19) rawDate.substring(0, 19) else rawDate
        val parsedDate = isoFormat.parse(clean) ?: return "Recently"
        val diffMs = System.currentTimeMillis() - parsedDate.time
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        val diffDay = diffHour / 24

        when {
            diffSec < 60 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHour < 24 -> "${diffHour}h ago"
            diffDay < 7 -> "${diffDay}d ago"
            else -> {
                val outFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                outFormat.format(parsedDate)
            }
        }
    } catch (_: Exception) {
        "Recently"
    }
}
