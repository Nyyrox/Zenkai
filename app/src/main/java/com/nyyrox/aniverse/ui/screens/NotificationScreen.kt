package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttPanel
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.supabase.AdminUserSearchResult
import com.nyyrox.aniverse.data.supabase.AppNotification
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.ui.components.decorations.AvatarDecorationRenderer
import com.nyyrox.aniverse.ui.components.decorations.RoleBadgeRow
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun handleNotificationUrl(
    context: android.content.Context,
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
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot open link: $trimmed", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NotificationScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onNavigateToCommunity: (String) -> Unit = {},
    onNavigateToDirectChat: (String) -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val myProfile = vm.userProfile
    val isLoggedIn = vm.authState is AuthState.LoggedIn
    val currentUserId = (vm.authState as? AuthState.LoggedIn)?.userId

    val isCurrentUserAdmin = myProfile?.role == "ADMIN" || myProfile?.role == "OWNER" ||
            myProfile?.roles?.contains("ADMIN") == true || myProfile?.roles?.contains("OWNER") == true ||
            myProfile?.roles?.contains("MODERATOR") == true ||
            myProfile?.isAdmin == true

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedNotificationForDetails by remember { mutableStateOf<AppNotification?>(null) }
    val tabTitles = remember(isCurrentUserAdmin) {
        if (isCurrentUserAdmin) listOf("Personal", "Global", "Admin Dispatch") else listOf("Personal", "Global")
    }

    LaunchedEffect(Unit) {
        vm.loadNotifications()
    }

    val allNotifs = vm.notifications
    val personalNotifs = remember(allNotifs, currentUserId) {
        allNotifs.filter { it.userId != null && it.userId == currentUserId }
    }
    val globalNotifs = remember(allNotifs) {
        allNotifs.filter { it.userId == null }
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
                        tint = OttTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NOTIFICATIONS",
                            color = OttTextPrimary,
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
                                    color = OttTextPrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Mentions, replies & ZENKAI updates",
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
                            tint = OttTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }


                    IconButton(
                        onClick = { vm.testPushNotification() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Push Notification",
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Tabs Row ────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = OttTextPrimary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = if (selectedTab == 2) Color(0xFFE11D48) else Color(0xFF38BDF8),
                            height = 2.5.dp
                        )
                    }
                },
                divider = {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OttInk(0.09f)))
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val tabUnread = when (index) {
                        0 -> personalNotifs.count { !it.isRead }
                        1 -> globalNotifs.count { !it.isRead }
                        else -> 0
                    }
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (index == 2) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFE11D48) else OttTextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = title,
                                    color = if (isSelected) OttTextPrimary else OttTextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (tabUnread > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(OttPrimary)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // ── Tab Content ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Personal Notifications
                        if (!isLoggedIn) {
                            GuestNotificationPrompt(onNavigateToLogin = onNavigateToLogin)
                        } else if (personalNotifs.isEmpty() && !vm.isNotificationsLoading) {
                            EmptyNotificationState(
                                icon = Icons.Default.NotificationsActive,
                                title = "No Personal Notifications",
                                subtitle = "When someone replies to your messages or mentions you in the community chat, it'll appear here."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(personalNotifs, key = { it.id }) { notif ->
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
                    }
                    1 -> {
                        // Global Notifications / Announcements
                        if (globalNotifs.isEmpty() && !vm.isNotificationsLoading) {
                            EmptyNotificationState(
                                icon = Icons.Default.Campaign,
                                title = "No Global Announcements",
                                subtitle = "System broadcasts, major releases, and community announcements will be posted here."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(globalNotifs, key = { it.id }) { notif ->
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
                    }
                    2 -> {
                        // Admin Dispatch Center (Admin Only)
                        AdminNotificationDispatchPanel(
                            vm = vm,
                            onNavigateToProfile = { onNavigate("public_profile:$it") }
                        )
                    }
                }

                if (vm.isNotificationsLoading) {
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
        "global", "announcement" -> Color(0xFFF59E0B)
        "admin" -> Color(0xFFE11D48)
        "system" -> Color(0xFF10B981)
        "reward" -> Color(0xFF10B981)
        "alert" -> Color(0xFFEF4444)
        "update" -> Color(0xFF8B5CF6)
        else -> Color(0xFF818CF8)
    }

    val cardBg = if (isRead) {
        OttPanelHigh
    } else {
        OttPanelHigh
    }

    val cardBorder = if (isRead) {
        OttInk(0.09f)
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
                modifier = Modifier
                    .size(42.dp),
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
                                .background(Brush.linearGradient(listOf(Color(0xFF2E3440), OttPanelHigh))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (notification.type) {
                                    "global", "announcement" -> Icons.Default.Campaign
                                    "reward" -> Icons.Default.CardGiftcard
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = OttTextPrimary,
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
                            color = OttTextPrimary,
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
                            modifier = Modifier
                                .size(26.dp)
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
                            .background(OttPanelHigh)
                            .border(0.5.dp, OttInk(0.20f), RoundedCornerShape(10.dp)),
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
                                        tint = OttTextPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = notification.actionLabel ?: "Claim Reward (+100 EXP)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OttTextPrimary
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
                                        tint = OttPanel,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = notification.actionLabel?.takeIf { notification.type != "reward" } ?: "Open Link",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OttPanel
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
    val isRead = notification.isRead
    val isClaimed = notification.isClaimed
    val timeAgo = remember(notification.createdAt) {
        formatNotificationTimestamp(notification.createdAt)
    }

    val typeBadgeColor = when (notification.type) {
        "mention", "reply" -> Color(0xFF38BDF8)
        "global", "announcement" -> Color(0xFFF59E0B)
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
                    .background(OttPanelHigh)
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
                                            .background(Brush.linearGradient(listOf(Color(0xFF2E3440), OttPanelHigh))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (notification.type) {
                                                "global", "announcement" -> Icons.Default.Campaign
                                                "reward" -> Icons.Default.CardGiftcard
                                                else -> Icons.Default.Person
                                            },
                                            contentDescription = null,
                                            tint = OttTextPrimary,
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
                                color = OttTextPrimary
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
                            color = OttTextPrimary
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
                                .border(1.dp, OttInk(0.13f), RoundedCornerShape(14.dp))
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
fun AdminNotificationDispatchPanel(
    vm: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var dispatchMode by remember { mutableIntStateOf(0) } // 0: Broadcast Global, 1: Direct User, 2: Welcome Template
    var titleText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var imageUrlText by remember { mutableStateOf("") }
    var actionUrlText by remember { mutableStateOf("") }
    var actionLabelText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("global") }

    val defaultBananaArt = "android.resource://${context.packageName}/drawable/welcome_nano_banana"

    // Welcome Template Form State
    var welcomeTitleText by remember(vm.welcomeConfig) { mutableStateOf(vm.welcomeConfig.title) }
    var welcomeMessageText by remember(vm.welcomeConfig) { mutableStateOf(vm.welcomeConfig.message) }
    var welcomeImageUrlText by remember(vm.welcomeConfig) { mutableStateOf(vm.welcomeConfig.imageUrl) }
    var welcomeActionUrlText by remember(vm.welcomeConfig) { mutableStateOf(vm.welcomeConfig.actionUrl) }
    var welcomeActionLabelText by remember(vm.welcomeConfig) { mutableStateOf(vm.welcomeConfig.actionLabel) }

    val categories = remember {
        listOf(
            "global" to "Global",
            "announcement" to "Announcement",
            "update" to "Update",
            "alert" to "Alert",
            "reward" to "Gift / Reward",
            "admin" to "Admin Notice",
            "system" to "System"
        )
    }

    // User search state
    var searchQuery by remember { mutableStateOf("") }
    var selectedTargetUser by remember { mutableStateOf<AdminUserSearchResult?>(null) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OttPanelHigh)
                    .border(1.dp, OttInk(0.16f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (dispatchMode == 0) Color(0xFFE11D48) else Color.Transparent)
                        .clickable {
                            dispatchMode = 0
                            if (selectedType == "admin") selectedType = "global"
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Broadcast",
                        color = OttTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (dispatchMode == 1) Color(0xFF38BDF8) else Color.Transparent)
                        .clickable {
                            dispatchMode = 1
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Direct User",
                        color = if (dispatchMode == 1) OttPanel else OttTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (dispatchMode == 2) Color(0xFFF59E0B) else Color.Transparent)
                        .clickable {
                            dispatchMode = 2
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Welcome Setup",
                        color = if (dispatchMode == 2) OttPanel else OttTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── MODE 2: WELCOME NOTIFICATION TEMPLATE SETUP ─────────────────────
        if (dispatchMode == 2) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OttPanelHigh)
                        .border(1.dp, Color(0x30F59E0B), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🍌", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "WELCOME NOTIFICATION SETTINGS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "Triggered when a new user registers for the first time. Edit the greeting text, image, action button, and test it.",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "WELCOME TITLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = welcomeTitleText,
                        onValueChange = { welcomeTitleText = it },
                        placeholder = { Text("Welcome to Zenkai", color = OttTextMuted, fontSize = 12.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "WELCOME MESSAGE BODY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = welcomeMessageText,
                        onValueChange = { welcomeMessageText = it },
                        placeholder = { Text("Welcome to Zenkai\nWatch Read Explore\nHave a happy journey", color = OttTextMuted, fontSize = 12.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        )
                    )
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "WELCOME NOTIFICATION IMAGE (CUSTOM URL OR PRESET)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    OutlinedTextField(
                        value = welcomeImageUrlText,
                        onValueChange = { welcomeImageUrlText = it },
                        placeholder = { Text("Leave blank for default or paste custom URL...", color = OttTextMuted, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { welcomeImageUrlText = defaultBananaArt },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22F59E0B), contentColor = Color(0xFFF59E0B)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Nano Banana Art 🍌", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (welcomeImageUrlText.isNotBlank()) {
                            Button(
                                onClick = { welcomeImageUrlText = "" },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22EF4444), contentColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Clear Image", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val effectivePreviewImg = welcomeImageUrlText.ifBlank { defaultBananaArt }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OttPanel)
                            .border(1.dp, OttInk(0.13f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = effectivePreviewImg,
                            contentDescription = "Welcome Preview Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ACTION BUTTON & REDIRECT LINK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )

                    OutlinedTextField(
                        value = welcomeActionLabelText,
                        onValueChange = { welcomeActionLabelText = it },
                        placeholder = { Text("Claim +100 Starter EXP", color = OttTextMuted, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = welcomeActionUrlText,
                        onValueChange = { welcomeActionUrlText = it },
                        placeholder = { Text("zenkai://rewards/welcome", color = OttTextMuted, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            // Live Notification Mockup Preview
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OttPanelHigh)
                        .border(1.dp, OttInk(0.19f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LIVE NOTIFICATION PREVIEW",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = welcomeImageUrlText.ifBlank { defaultBananaArt },
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(OttInk(0.13f)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = welcomeTitleText.ifBlank { "Welcome to Zenkai" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OttTextPrimary
                            )
                            Text(
                                text = "From: Nano Banana 🍌",
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Text(
                        text = welcomeMessageText.ifBlank { "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey" },
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 16.sp
                    )

                    if (welcomeImageUrlText.isNotBlank()) {
                        AsyncImage(
                            model = welcomeImageUrlText,
                            contentDescription = "Welcome Media Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2333)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (welcomeActionLabelText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = welcomeActionLabelText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            if (statusFeedback != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x2210B981))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(text = statusFeedback!!, color = Color(0xFF6EE7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Save & Test Buttons
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val updated = com.nyyrox.aniverse.data.WelcomeNotificationConfig(
                                title = welcomeTitleText.trim().ifBlank { "Welcome to Zenkai" },
                                message = welcomeMessageText.trim().ifBlank { "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey" },
                                imageUrl = welcomeImageUrlText.trim(),
                                actionLabel = welcomeActionLabelText.trim().ifBlank { "Claim +100 Starter EXP" },
                                actionUrl = welcomeActionUrlText.trim().ifBlank { "zenkai://rewards/welcome" }
                            )
                            vm.saveWelcomeTemplateConfig(updated) { success ->
                                statusFeedback = if (success) "Welcome template saved to cloud & device!" else "Welcome template saved locally."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = OttPanel)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Welcome Template", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val updated = com.nyyrox.aniverse.data.WelcomeNotificationConfig(
                                title = welcomeTitleText.trim().ifBlank { "Welcome to Zenkai" },
                                message = welcomeMessageText.trim().ifBlank { "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey" },
                                imageUrl = welcomeImageUrlText.trim(),
                                actionLabel = welcomeActionLabelText.trim().ifBlank { "Claim +100 Starter EXP" },
                                actionUrl = welcomeActionUrlText.trim().ifBlank { "zenkai://rewards/welcome" }
                            )
                            vm.saveWelcomeTemplateConfig(updated)
                            vm.testWelcomeNotification()
                            statusFeedback = "Test Welcome notification dispatched to your account!"
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x2038BDF8), contentColor = Color(0xFF38BDF8)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Welcome Notification", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // ── MODES 0 & 1: BROADCAST / DIRECT NOTIFICATION ─────────────────
            // Category Type Selector Pills
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "NOTIFICATION CATEGORY & STYLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { (key, label) ->
                            val isSel = selectedType == key
                            val catColor = when (key) {
                                "reward" -> Color(0xFF10B981)
                                "alert" -> Color(0xFFEF4444)
                                "update" -> Color(0xFF8B5CF6)
                                "announcement", "global" -> Color(0xFFF59E0B)
                                "admin" -> Color(0xFFE11D48)
                                else -> Color(0xFF38BDF8)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) catColor else OttInk(0.09f))
                                    .border(1.dp, if (isSel) catColor else OttInk(0.16f), RoundedCornerShape(20.dp))
                                    .clickable { selectedType = key }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) (if (key == "admin" || key == "reward" || key == "alert" || key == "update") OttTextPrimary else OttPanel) else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }

            // Direct User Search Box (if mode == 1)
            if (dispatchMode == 1) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SELECT RECIPIENT USER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (selectedTargetUser == null) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    vm.adminSearchUsers(it)
                                },
                                placeholder = {
                                    Text("Search user by Username, Display Name or Email...", color = OttTextMuted, fontSize = 12.5.sp)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            vm.adminSearchUsers("")
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = OttPanelHigh,
                                    unfocusedContainerColor = OttPanelHigh,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = OttInk(0.16f),
                                    focusedTextColor = OttTextPrimary,
                                    unfocusedTextColor = OttTextPrimary
                                ),
                                singleLine = true
                            )

                            // Search Results List
                            if (vm.isAdminSearching) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            } else if (vm.adminSearchResults.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(OttPanelHigh)
                                        .border(1.dp, OttInk(0.20f), RoundedCornerShape(12.dp))
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    vm.adminSearchResults.forEach { u ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedTargetUser = u
                                                    searchQuery = ""
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = u.avatarUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp).clip(CircleShape).background(OttInk(0.13f)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = u.displayName ?: u.username ?: "Otaku",
                                                    color = OttTextPrimary,
                                                    fontSize = 12.sp,
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
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0x2238BDF8))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Select", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Selected User Card with Profile View Shortcut
                            val target = selectedTargetUser!!
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF152238))
                                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = target.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(OttInk(0.13f)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = target.displayName ?: target.username ?: "Otaku",
                                        color = OttTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = listOfNotNull(target.username?.let { "@$it" }, target.email).joinToString(" • "),
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp
                                    )
                                }
                                if (onNavigateToProfile != null) {
                                    Button(
                                        onClick = { onNavigateToProfile(target.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3038BDF8), contentColor = Color(0xFF38BDF8)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(if (vm.isCurrentUserOwner()) "👑 Roles" else "Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                IconButton(onClick = { selectedTargetUser = null }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Deselect", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Title Input
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "NOTIFICATION TITLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        placeholder = {
                            Text(if (dispatchMode == 0) "e.g. Major Zenkai Update v2.5 is live!" else "e.g. Account Notice / Reward Alert", color = OttTextMuted, fontSize = 12.5.sp)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = if (dispatchMode == 0) Color(0xFFE11D48) else Color(0xFF38BDF8),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            // Message Body Input
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "NOTIFICATION BODY / CONTENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = {
                            Text("Write message details to send directly to recipient's notification tray and app...", color = OttTextMuted, fontSize = 12.5.sp)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = if (dispatchMode == 0) Color(0xFFE11D48) else Color(0xFF38BDF8),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        )
                    )
                }
            }

            // Optional Media Image URL Input
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ATTACH MEDIA IMAGE URL (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = imageUrlText,
                        onValueChange = { imageUrlText = it },
                        placeholder = {
                            Text("https://images.unsplash.com/... or poster URL", color = OttTextMuted, fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            // Optional Action Link & Action Button Label
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ACTION BUTTON & LINK (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )

                    OutlinedTextField(
                        value = actionUrlText,
                        onValueChange = { actionUrlText = it },
                        placeholder = {
                            Text("Action URL: https://... or zenkai://rewards/...", color = OttTextMuted, fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = actionLabelText,
                        onValueChange = { actionLabelText = it },
                        placeholder = {
                            Text("Action Button Label: e.g. Claim 500 EXP / Open Stream / View Event", color = OttTextMuted, fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OttPanelHigh,
                            unfocusedContainerColor = OttPanelHigh,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = OttInk(0.16f),
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            // Feedback message
            if (statusFeedback != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x2210B981))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(text = statusFeedback!!, color = Color(0xFF6EE7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Submit Button
            item {
                val canSend = (dispatchMode == 0 || selectedTargetUser != null) &&
                        titleText.isNotBlank() && messageText.isNotBlank() && !vm.isSendingAdminNotification

                Button(
                    onClick = {
                        if (dispatchMode == 0) {
                            vm.sendAdminBroadcast(
                                title = titleText,
                                message = messageText,
                                type = selectedType,
                                imageUrl = imageUrlText.ifBlank { null },
                                actionUrl = actionUrlText.ifBlank { null },
                                actionLabel = actionLabelText.ifBlank { null },
                                onComplete = { success ->
                                    if (success) {
                                        statusFeedback = "Global Broadcast notification sent successfully!"
                                        titleText = ""
                                        messageText = ""
                                        imageUrlText = ""
                                        actionUrlText = ""
                                        actionLabelText = ""
                                    } else {
                                        statusFeedback = "Failed to send broadcast notification. Check permissions."
                                    }
                                }
                            )
                        } else {
                            val target = selectedTargetUser ?: return@Button
                            vm.sendAdminDirectNotification(
                                targetUserId = target.id,
                                title = titleText,
                                message = messageText,
                                type = selectedType,
                                imageUrl = imageUrlText.ifBlank { null },
                                actionUrl = actionUrlText.ifBlank { null },
                                actionLabel = actionLabelText.ifBlank { null },
                                onComplete = { success ->
                                    if (success) {
                                        statusFeedback = "Direct Notification sent successfully to @${target.displayName ?: target.username}!"
                                        titleText = ""
                                        messageText = ""
                                        imageUrlText = ""
                                        actionUrlText = ""
                                        actionLabelText = ""
                                        selectedTargetUser = null
                                    } else {
                                        statusFeedback = "Failed to send direct notification."
                                    }
                                }
                            )
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dispatchMode == 0) Color(0xFFE11D48) else Color(0xFF38BDF8),
                        disabledContainerColor = OttInk(0.13f)
                    )
                ) {
                    if (vm.isSendingAdminNotification) {
                        CircularProgressIndicator(color = OttTextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (dispatchMode == 1) OttPanel else OttTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (dispatchMode == 0) "SEND BROADCAST NOTIFICATION" else "SEND DIRECT NOTIFICATION",
                            color = if (dispatchMode == 1) OttPanel else OttTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
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
            color = OttTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = OttTextMuted,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            color = OttTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Sign in to receive community mentions, replies, and direct alerts sent to your profile.",
            color = OttTextMuted,
            fontSize = 12.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToLogin,
            colors = ButtonDefaults.buttonColors(containerColor = OttPrimary),
            shape = OttPillShape
        ) {
            Text("Sign In Now", color = OttTextPrimary, fontWeight = FontWeight.Bold)
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
