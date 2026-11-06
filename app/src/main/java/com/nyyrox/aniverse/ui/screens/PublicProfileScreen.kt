package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.ui.components.decorations.ApngDecorationImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.nyyrox.aniverse.data.LevelAndStreakSystem
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.data.supabase.CloudProfile
import com.nyyrox.aniverse.data.supabase.CloudWatchlistItem
import com.nyyrox.aniverse.data.supabase.CloudHistoryItem
import com.nyyrox.aniverse.ui.components.decorations.AvatarDecorationRenderer
import com.nyyrox.aniverse.ui.components.decorations.ProfileBannerRenderer
import com.nyyrox.aniverse.ui.components.decorations.TextEffectRenderer
import com.nyyrox.aniverse.ui.components.decorations.RoleBadgeRow
import com.nyyrox.aniverse.ui.components.decorations.LevelRankTagBadge
import com.nyyrox.aniverse.ui.components.decorations.AnimatedLevelThemeBadge
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.ui.components.decorations.AnimatedStreakBadge
import com.nyyrox.aniverse.ui.components.FavoriteAnimeShowcaseCard
import com.nyyrox.aniverse.ui.components.PublicTimeSpentShowcase
import com.nyyrox.aniverse.ui.components.getBioTextStyle
import com.nyyrox.aniverse.data.manga.MangaBookmarkItem
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.novel.NovelHistoryItem
import com.nyyrox.aniverse.ui.theme.*
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel

@Composable
fun PublicProfileScreen(
    userId: String,
    vm: MainViewModel,
    onBack: () -> Unit,
    onAnimeClick: ((Int) -> Unit)? = null,
    onMangaClick: ((String) -> Unit)? = null,
    onNovelClick: ((String) -> Unit)? = null,
    onDirectChatClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var cloudProfile by remember { mutableStateOf<CloudProfile?>(null) }
    var publicWatchlist by remember { mutableStateOf<List<CloudWatchlistItem>>(emptyList()) }
    var publicWatchHistory by remember { mutableStateOf<List<CloudHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isWatchlistLoading by remember { mutableStateOf(true) }
    var isWatchHistoryLoading by remember { mutableStateOf(false) }
    var isBioExpanded by remember { mutableStateOf(false) }

    // Owner role and admin management state
    var targetIsAdmin by remember(cloudProfile) {
        mutableStateOf(cloudProfile?.isAdmin == true || cloudProfile?.role == "ADMIN" || cloudProfile?.roles?.contains("ADMIN") == true)
    }
    var targetRoles by remember(cloudProfile) {
        mutableStateOf(cloudProfile?.roles ?: emptyList())
    }
    var ownerStatusMsg by remember { mutableStateOf<String?>(null) }

    val colors = LocalAppColors.current
    val isDark = LocalIsDark.current
    val pageBg = colors.bg
    val cardSurface = colors.surfaceElevated
    val cardBorder = colors.glass.border
    val textPrimary = colors.textPrimary
    val textSecondary = colors.textSecondary
    val textMuted = colors.textMuted

    LaunchedEffect(userId) {
        isLoading = true
        vm.fetchPublicProfile(userId) { profile ->
            cloudProfile = profile
            isLoading = false
        }
        isWatchlistLoading = true
        vm.fetchPublicWatchlist(userId) { list ->
            publicWatchlist = list
            isWatchlistLoading = false
        }
        isWatchHistoryLoading = true
        vm.fetchPublicWatchHistory(userId) { history ->
            publicWatchHistory = history
            isWatchHistoryLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE11D48), modifier = Modifier.size(36.dp))
            }
        } else {
            val profile = cloudProfile
            val displayName = profile?.displayName?.ifBlank { "Otaku" } ?: "Otaku"
            val bannerUrl = profile?.bannerUrl
            val avatarUrl = profile?.avatarUrl
            val bannerTheme = profile?.bannerTheme
            val avatarDeco = profile?.avatarDecoration
            val avatarFrame = profile?.avatarFrame
            val textEffect = profile?.textEffect
            val effect = profile?.profileEffect
            val nameplate = profile?.nameplate
            val nameplateEmoji = profile?.nameplateEmoji
            val bio = profile?.bio
            val otakuRank = profile?.otakuRank ?: "Genin"
            val favoriteAnime = profile?.favoriteAnime

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Box containing Banner + naturally overlapping Avatar
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Banner + Effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        ProfileBannerRenderer(
                            bannerThemeId = bannerTheme,
                            customBannerUrl = bannerUrl,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Back button on top left
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x88000000) else Color(0xCCFFFFFF))
                                .border(1.dp, cardBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary
                            )
                        }

                        // Bottom gradient fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, pageBg.copy(alpha = 0.5f), pageBg)
                                    )
                                )
                        )
                    }

                    // Avatar with Decoration + Bio Quote Bubble
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 160.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AvatarDecorationRenderer(
                            decorationId = avatarDeco,
                            frameId = avatarFrame,
                            size = 84.dp
                        ) {
                            if (!avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
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
                                        .background(Color(0xFF2E3440)),
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

                        val isBioLong = !bio.isNullOrBlank() && (bio.length > 55 || bio.contains("\n"))

                        if (!bio.isNullOrBlank() && !isBioLong) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "\u201c",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OttPrimary,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$bio\u201d",
                                        style = getBioTextStyle(profile?.bioFont).copy(
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = textPrimary
                                        ),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Username with Nameplate
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextEffectRenderer(
                            effectId = textEffect,
                            text = displayName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RoleBadgeRow(
                            roles = profile?.roles,
                            isAdmin = profile?.isAdmin,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val userLevel = profile?.level ?: 1
                    val userExp = profile?.exp ?: 0L
                    val userStreak = profile?.streakCount ?: 1
                    val userLongest = profile?.longestStreak ?: userStreak
                    val effectiveTag = profile?.customTag?.takeIf {
                        it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true)
                    }

                    // Level, Rank Tag & Streak Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Level Badge with Escalating Kinetic Animations
                        AnimatedLevelThemeBadge(level = userLevel)

                        // Level-Scaled Animated Tag (Only shown if equipped and valid)
                        if (effectiveTag != null) {
                            LevelRankTagBadge(
                                tag = effectiveTag,
                                level = userLevel
                            )
                        }

                        // Daily Streak - Animated Badge
                        if (userStreak > 0) {
                            AnimatedStreakBadge(
                                streakCount = userStreak,
                                isFullLabel = true
                            )
                        }
                    }

                    // Live Activity Status Banner & Direct Message Button
                    val isOnline = isUserOnline(profile?.lastSeenAt) && profile?.isOnline != false
                    val actText = if (isOnline) profile?.currentActivity?.ifBlank { null } else null
                    val actType = profile?.currentActivityType ?: "ANIME"
                    val actTargetId = profile?.currentActivityTargetId

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOnline) Color(0x1810B981) else Color(0x10FFFFFF))
                            .border(
                                1.dp,
                                if (isOnline) Color(0x4410B981) else Color(0x20FFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !actTargetId.isNullOrBlank()) {
                                if (actTargetId != null) {
                                    when (actType) {
                                        "ANIME" -> actTargetId.toIntOrNull()?.let { onAnimeClick?.invoke(it) }
                                        "MANGA" -> onMangaClick?.invoke(actTargetId)
                                        "NOVEL" -> onNovelClick?.invoke(actTargetId)
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF10B981) else Color(0xFF64748B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                if (!actText.isNullOrBlank()) {
                                    val icon = when (actType) {
                                        "ANIME" -> "🎬 Watching"
                                        "MANGA" -> "📖 Reading"
                                        "NOVEL" -> "📜 Reading"
                                        "COMMUNITY" -> "💬 In Lounge"
                                        else -> "✨ Active"
                                    }
                                    Text(
                                        text = "$icon: $actText",
                                        color = if (isOnline) Color(0xFF34D399) else Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (actTargetId != null) {
                                        Text(
                                            text = "Tap to view media",
                                            color = OttPrimary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (isOnline) "Online in ZENKAI" else formatLastSeen(profile?.lastSeenAt),
                                        color = if (isOnline) Color(0xFF34D399) else OttTextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Direct Message button on profile
                        if (onDirectChatClick != null) {
                            Button(
                                onClick = { onDirectChatClick(userId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Dedicated Clean Bio Text for Longer Bios (No border, no background)
                    val isBioLongCard = !bio.isNullOrBlank() && (bio.length > 55 || bio.contains("\n"))
                    if (isBioLongCard && !bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "“",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OttPrimary,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$bio”",
                                    style = getBioTextStyle(profile?.bioFont).copy(
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        color = Color(0xFFF1F5F9)
                                    ),
                                    maxLines = if (isBioExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (bio.length > 120 || bio.lines().size > 3) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBioExpanded) "Show less" else "Read more",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.clickable { isBioExpanded = !isBioExpanded }
                                )
                            }
                        }
                    }

                    // ── OWNER CONTROL: MANAGE ADMIN & ROLES (Only visible to App Owner) ──
                    if (vm.isCurrentUserOwner() && userId != vm.userProfile.id) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141722))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "👑", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "OWNER CONTROL: PERMISSIONS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFF59E0B),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Promote/demote Admin & assign community roles",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (targetIsAdmin) Color(0x30E11D48) else Color(0x2064748B))
                                        .border(1.dp, if (targetIsAdmin) Color(0xFFE11D48) else Color(0x4064748B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (targetIsAdmin) "ADMINISTRATOR" else "MEMBER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (targetIsAdmin) Color(0xFFFB7185) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Quick Action: Promote or Demote from Admin Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!targetIsAdmin) {
                                    Button(
                                        onClick = {
                                            targetIsAdmin = true
                                            if (!targetRoles.contains("ADMIN")) {
                                                targetRoles = targetRoles + "ADMIN"
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Promote to Admin", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            targetIsAdmin = false
                                            targetRoles = targetRoles - "ADMIN"
                                        },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48), contentColor = Color.White)
                                    ) {
                                        Text("Demote from Admin", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Roles Chips Multi-Selector
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "ASSIGN ROLES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1)
                                )
                                val availableRoles = listOf("ADMIN", "MODERATOR", "VIP", "SUPPORTER", "DONATOR", "OWNER")
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(availableRoles) { roleItem ->
                                        val isSelected = targetRoles.contains(roleItem)
                                        val chipColor = when (roleItem) {
                                            "OWNER" -> Color(0xFFF59E0B)
                                            "ADMIN" -> Color(0xFFE11D48)
                                            "MODERATOR" -> Color(0xFF38BDF8)
                                            "VIP" -> Color(0xFF8B5CF6)
                                            else -> Color(0xFF10B981)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) chipColor.copy(alpha = 0.25f) else Color(0x18FFFFFF))
                                                .border(1.dp, if (isSelected) chipColor else Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    targetRoles = if (isSelected) targetRoles - roleItem else targetRoles + roleItem
                                                    if (roleItem == "ADMIN") {
                                                        targetIsAdmin = !isSelected
                                                    }
                                                }
                                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = roleItem,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) chipColor else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }

                            // Status Message feedback if present
                            if (ownerStatusMsg != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x2210B981))
                                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(text = ownerStatusMsg!!, color = Color(0xFF6EE7B7), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Save Button
                            Button(
                                onClick = {
                                    ownerStatusMsg = null
                                    vm.updateTargetUserRolesAndAdmin(
                                        targetUserId = userId,
                                        targetUsername = displayName,
                                        isAdmin = targetIsAdmin,
                                        roles = targetRoles
                                    ) { success ->
                                        if (success) {
                                            cloudProfile = cloudProfile?.copy(
                                                isAdmin = targetIsAdmin,
                                                roles = targetRoles,
                                                role = if (targetIsAdmin) "ADMIN" else if (targetRoles.contains("OWNER")) "OWNER" else if (targetRoles.contains("MODERATOR")) "MODERATOR" else "MEMBER"
                                            )
                                            ownerStatusMsg = "Permissions successfully updated for @$displayName!"
                                        } else {
                                            ownerStatusMsg = "Failed to update permissions. Check connection."
                                        }
                                    }
                                },
                                enabled = !vm.isUpdatingUserRoles,
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    contentColor = Color(0xFF090B10)
                                )
                            ) {
                                if (vm.isUpdatingUserRoles) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save User Permissions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Level Progress Card
                    val nextLvlBase = LevelAndStreakSystem.expForNextLevel(userLevel)
                    val progress = LevelAndStreakSystem.calculateLevelProgress(userExp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Level $userLevel Progression",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = "$userExp / $nextLvlBase EXP",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OttAccentGold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val tierGradient = remember(userLevel) {
                                when {
                                    userLevel >= 100 -> Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFFFFF), Color(0xFFF59E0B)))
                                    userLevel >= 80 -> Brush.horizontalGradient(listOf(Color(0xFFC084FC), Color(0xFFE9D5FF), Color(0xFF818CF8)))
                                    userLevel >= 40 -> Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFFB7185), Color(0xFFF59E0B)))
                                    userLevel >= 15 -> Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF34D399)))
                                    else -> Brush.horizontalGradient(listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0)))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0x28FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(tierGradient)
                                )
                            }
                        }
                    }



                    // Public Time Spent Showcase
                    val animeTime = profile?.timeSpentAnime ?: 0L
                    val mangaTime = profile?.timeSpentManga ?: 0L
                    val novelTime = profile?.timeSpentNovel ?: 0L
                    val appTime = profile?.timeSpentApp ?: 0L
                    val totalTime = profile?.timeSpentTotal ?: 0L
                    val weeklyTime = profile?.weeklyTimeSpent ?: 0L
                    val dailyTime = profile?.dailyTimeSpent ?: 0L

                    Spacer(modifier = Modifier.height(14.dp))
                    PublicTimeSpentShowcase(
                        timeSpentAnime = animeTime,
                        timeSpentManga = mangaTime,
                        timeSpentNovel = novelTime,
                        timeSpentApp = appTime,
                        timeSpentTotal = totalTime,
                        weeklyTimeSpent = weeklyTime,
                        dailyTimeSpent = dailyTime
                    )

                    if (!favoriteAnime.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        FavoriteAnimeShowcaseCard(
                            favoriteAnime = favoriteAnime,
                            onAnimeClick = onAnimeClick
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val myUserId = (vm.authState as? AuthState.LoggedIn)?.userId
                    val isMyOwnProfile = myUserId != null && myUserId == userId

                    // Public Watch History Showcase (or Private Notice)
                    val isHistoryPrivate = profile?.isHistoryPrivate == true
                    if (isHistoryPrivate && !isMyOwnProfile) {
                        PrivateSectionCard(
                            title = "WATCH HISTORY IS PRIVATE",
                            subtitle = "@$displayName has set their anime watch history to private"
                        )
                    } else {
                        PublicWatchHistoryShowcase(
                            history = publicWatchHistory,
                            isLoading = isWatchHistoryLoading,
                            onAnimeClick = onAnimeClick
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Public Watchlist Showcase (or Private Notice)
                    val isWatchlistPrivate = profile?.isWatchlistPrivate == true
                    if (isWatchlistPrivate && !isMyOwnProfile) {
                        PrivateSectionCard(
                            title = "ANIME WATCHLIST IS PRIVATE",
                            subtitle = "@$displayName has set their anime watchlist to private"
                        )
                    } else {
                        PublicWatchlistShowcase(
                            watchlist = publicWatchlist,
                            isLoading = isWatchlistLoading,
                            onAnimeClick = onAnimeClick
                        )
                    }

                    // Public Manga & Novel Reading History (or Private Notice)
                    val isMangaPrivate = (profile?.isMangaPrivate == true) || (profile?.isNovelPrivate == true)
                    val publicMangaHistory = remember(profile?.mangaHistoryJson) {
                        vm.parsePublicMangaHistory(profile?.mangaHistoryJson)
                    }
                    val publicNovelHistory = remember(profile?.novelHistoryJson) {
                        vm.parsePublicNovelHistory(profile?.novelHistoryJson)
                    }
                    val publicMangaBookmarks = remember(profile?.mangaBookmarksJson) {
                        vm.parsePublicMangaBookmarks(profile?.mangaBookmarksJson)
                    }

                    if (isMangaPrivate && !isMyOwnProfile) {
                        Spacer(modifier = Modifier.height(14.dp))
                        PrivateSectionCard(
                            title = "MANGA & NOVEL READING IS PRIVATE",
                            subtitle = "@$displayName has set their manga and novel reading history to private"
                        )
                    } else {
                        // Public Manga History Showcase
                        if (publicMangaHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            PublicMangaHistoryShowcase(
                                mangaHistory = publicMangaHistory,
                                onMangaClick = onMangaClick
                            )
                        }

                        // Public Novel History Showcase
                        if (publicNovelHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            PublicNovelHistoryShowcase(
                                novelHistory = publicNovelHistory,
                                onNovelClick = onNovelClick
                            )
                        }

                        // Public Manga Bookmarks Showcase
                        if (publicMangaBookmarks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            PublicMangaBookmarksShowcase(
                                bookmarks = publicMangaBookmarks,
                                onMangaClick = onMangaClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

@Composable
fun PrivateSectionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
                    .border(1.dp, Color(0x30FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🔒", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = Color(0xFFCBD5E1)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = OttTextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun PublicWatchlistShowcase(
    watchlist: List<CloudWatchlistItem>,
    isLoading: Boolean,
    onAnimeClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Watching", "Completed", "Plan to Watch", "On Hold", "Dropped")

    val filteredList = remember(watchlist, selectedFilter) {
        when (selectedFilter) {
            "Watching" -> watchlist.filter { it.status.equals("watching", ignoreCase = true) }
            "Completed" -> watchlist.filter { it.status.equals("completed", ignoreCase = true) }
            "Plan to Watch" -> watchlist.filter { it.status.equals("plan_to_watch", ignoreCase = true) }
            "On Hold" -> watchlist.filter { it.status.equals("on_hold", ignoreCase = true) }
            "Dropped" -> watchlist.filter { it.status.equals("dropped", ignoreCase = true) }
            else -> watchlist
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PUBLIC WATCHLIST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = OttSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${watchlist.size} Series Saved",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                val count = when (filter) {
                    "Watching" -> watchlist.count { it.status.equals("watching", ignoreCase = true) }
                    "Completed" -> watchlist.count { it.status.equals("completed", ignoreCase = true) }
                    "Plan to Watch" -> watchlist.count { it.status.equals("plan_to_watch", ignoreCase = true) }
                    "On Hold" -> watchlist.count { it.status.equals("on_hold", ignoreCase = true) }
                    "Dropped" -> watchlist.count { it.status.equals("dropped", ignoreCase = true) }
                    else -> watchlist.size
                }

                Box(
                    modifier = Modifier
                        .clip(OttPillShape)
                        .background(
                            if (isSelected) Color(0xFFE11D48).copy(alpha = 0.25f)
                            else Color(0x14FFFFFF)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFFE11D48) else Color(0x1CFFFFFF),
                            OttPillShape
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = filter,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else OttTextSecondary
                        )
                        if (count > 0) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "($count)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFFFD1DC) else OttTextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE11D48), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else if (watchlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No public anime on this watchlist",
                    fontSize = 12.sp,
                    color = OttTextMuted
                )
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No anime marked as $selectedFilter",
                    fontSize = 12.sp,
                    color = OttTextMuted
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredList, key = { it.animeId }) { item ->
                    PublicWatchlistCard(
                        item = item,
                        onClick = { onAnimeClick?.invoke(item.animeId) }
                    )
                }
            }
        }
    }
}

@Composable
fun PublicWatchlistCard(
    item: CloudWatchlistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (item.status?.lowercase()) {
        "watching" -> Color(0xFF10B981)
        "plan_to_watch" -> Color(0xFF3B82F6)
        "completed" -> Color(0xFFA855F7)
        "dropped" -> Color(0xFFEF4444)
        "rewatching" -> Color(0xFFF59E0B)
        "on_hold" -> Color(0xFFEAB308)
        else -> Color(0xFF3B82F6)
    }
    val statusLabel = when (item.status?.lowercase()) {
        "watching" -> "Watching"
        "plan_to_watch" -> "Plan to Watch"
        "completed" -> "Completed"
        "dropped" -> "Dropped"
        "rewatching" -> "Rewatching"
        "on_hold" -> "On Hold"
        else -> "Watchlist"
    }

    Column(
        modifier = modifier
            .width(115.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OttSurfaceElevated)
                .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = item.cover,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient shadow at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xD0000000))
                        )
                    )
            )

            // Status Badge top-start
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .border(0.6.dp, statusColor.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = statusLabel,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Progress badge bottom-end
            val ep = item.progressEpisode ?: 0
            if (ep > 0 || (item.episodes != null && item.episodes > 0)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ep $ep${if (item.episodes != null && item.episodes > 0) "/${item.episodes}" else ""}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD1DC)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = item.title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val firstTag = item.customTags?.firstOrNull()?.takeIf { it.isNotBlank() }
        if (firstTag != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x28E11D48))
                    .border(0.6.dp, Color(0x60E11D48), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = firstTag,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF99AA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PublicWatchHistoryShowcase(
    history: List<CloudHistoryItem>,
    isLoading: Boolean,
    onAnimeClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty() && !isLoading) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECENT WATCH HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (history.isNotEmpty()) "${history.size} Episodes Watched" else "Watch History",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFCBD5E1), modifier = Modifier.size(24.dp))
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(history, key = { "${it.animeId}_${it.lastEpisode}" }) { item ->
                    Column(
                        modifier = Modifier
                            .width(115.dp)
                            .clickable { onAnimeClick?.invoke(item.animeId) }
                    ) {
                        Box(
                            modifier = Modifier
                                .width(115.dp)
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFFFFF))
                        ) {
                            AsyncImage(
                                model = item.cover.ifBlank { item.banner },
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Episode pill on top-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xDD000000))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (item.totalEpisodes != null && item.totalEpisodes > 0) {
                                        "EP ${item.lastEpisode}/${item.totalEpisodes}"
                                    } else {
                                        "EP ${item.lastEpisode}"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }

                            // Watch progress bar at bottom of card
                            val pos = item.positionMs ?: 0L
                            val dur = item.durationMs ?: 0L
                            if (dur > 0L && pos > 0L) {
                                val frac = (pos.toFloat() / dur.toFloat()).coerceIn(0.05f, 1f)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(Color(0x60000000))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(frac)
                                            .background(Color(0xFFE11D48))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PublicMangaHistoryShowcase(
    mangaHistory: List<MangaHistoryItem>,
    onMangaClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (mangaHistory.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECENT MANGA HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${mangaHistory.size} Chapters Read",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(mangaHistory, key = { it.mangaId }) { item ->
                Column(
                    modifier = Modifier
                        .width(115.dp)
                        .clickable { onMangaClick?.invoke(item.mangaId) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22FFFFFF))
                    ) {
                        AsyncImage(
                            model = item.poster ?: "",
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (item.chapterNumber.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xDD000000))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CH ${item.chapterNumber}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PublicNovelHistoryShowcase(
    novelHistory: List<NovelHistoryItem>,
    onNovelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (novelHistory.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECENT NOVEL HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${novelHistory.size} Novels Read",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(novelHistory, key = { it.slug }) { item ->
                Column(
                    modifier = Modifier
                        .width(115.dp)
                        .clickable { onNovelClick?.invoke(item.slug) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22FFFFFF))
                    ) {
                        AsyncImage(
                            model = item.cover,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (item.lastChapterTitle.isNotBlank() || item.lastChapterSlug.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xDD000000))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.lastChapterTitle.ifBlank { item.lastChapterSlug },
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PublicMangaBookmarksShowcase(
    bookmarks: List<MangaBookmarkItem>,
    onMangaClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (bookmarks.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SAVED MANGA BOOKMARKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFFA78BFA)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${bookmarks.size} Titles Bookmarked",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val uniqueBookmarks = bookmarks.distinctBy { "${it.bookmarkType.name}:${it.mangaId}" }
            items(uniqueBookmarks, key = { "${it.bookmarkType.name}:${it.mangaId}:${it.chapterId.orEmpty()}:${it.pageIndex ?: 0}" }) { item ->
                Column(
                    modifier = Modifier
                        .width(115.dp)
                        .clickable { onMangaClick?.invoke(item.mangaId) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22FFFFFF))
                    ) {
                        AsyncImage(
                            model = item.poster ?: "",
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xDD000000))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.type?.ifBlank { "Manga" } ?: "Manga",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
