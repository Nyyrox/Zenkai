package com.nyyrox.aniverse.ui.screens

import android.widget.Toast
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttPanel
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.AvatarBannerRepository
import com.nyyrox.aniverse.data.AvatarItem
import com.nyyrox.aniverse.data.BannerItem
import com.nyyrox.aniverse.data.decorations.DecorationItem
import com.nyyrox.aniverse.data.decorations.DecorationRarity
import com.nyyrox.aniverse.data.decorations.DecorationType
import com.nyyrox.aniverse.data.decorations.ZenkaiDecorationCatalog
import com.nyyrox.aniverse.ui.components.decorations.AvatarDecorationRenderer
import com.nyyrox.aniverse.ui.components.decorations.LevelRankTagBadge
import com.nyyrox.aniverse.ui.components.decorations.RarityBadge
import com.nyyrox.aniverse.ui.components.decorations.TextEffectRenderer
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel

@Composable
fun DecorationClosetScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile = vm.userProfile

    // Repository init
    LaunchedEffect(Unit) {
        AvatarBannerRepository.init(context)
    }

    val avatarsFromFlow by AvatarBannerRepository.avatarsFlow.collectAsState()
    val bannersFromFlow by AvatarBannerRepository.bannersFlow.collectAsState()

    val avatarList = remember(avatarsFromFlow) {
        if (avatarsFromFlow.isNotEmpty()) avatarsFromFlow else AvatarBannerRepository.getAvatars(context)
    }
    val bannerList = remember(bannersFromFlow) {
        if (bannersFromFlow.isNotEmpty()) bannersFromFlow else AvatarBannerRepository.getBanners(context)
    }

    val avatarCategories = remember(avatarList) { AvatarBannerRepository.getAvatarCategories(context) }
    val bannerCategories = remember(bannerList) { AvatarBannerRepository.getBannerCategories(context) }

    // Active preview selections
    var previewOverlayId by remember(userProfile.avatarDecorationId) { mutableStateOf(userProfile.avatarDecorationId) }
    var previewTextEffectId by remember(userProfile.textEffectId) { mutableStateOf(userProfile.textEffectId) }
    var previewAvatarUrl by remember(userProfile.avatarUrl) { mutableStateOf(userProfile.avatarUrl) }
    var previewBannerUrl by remember(userProfile.bannerUrl) { mutableStateOf(userProfile.bannerUrl) }

    // Navigation tabs: 0: Avatar Overlay, 1: Avatars, 2: Banners, 3: Text Effects
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = remember(avatarList.size, bannerList.size) {
        listOf(
            "Avatar Overlay (65)",
            "Avatars (${if (avatarList.isNotEmpty()) avatarList.size else 658})",
            "Banners (${if (bannerList.isNotEmpty()) bannerList.size else 104})",
            "Text Effects (16)"
        )
    }

    // Filters for tabs
    var selectedRarity by remember { mutableStateOf<DecorationRarity?>(null) }
    val rarityFilters = listOf(null) + DecorationRarity.values().toList()

    var selectedAvatarCategory by remember { mutableStateOf("All") }
    var selectedBannerCategory by remember { mutableStateOf("All") }

    val filteredAvatars = remember(avatarList, selectedAvatarCategory) {
        if (selectedAvatarCategory == "All") avatarList
        else avatarList.filter { item -> item.tags.any { it.equals(selectedAvatarCategory, ignoreCase = true) } }
    }

    val filteredBanners = remember(bannerList, selectedBannerCategory) {
        if (selectedBannerCategory == "All") bannerList
        else bannerList.filter { it.category.equals(selectedBannerCategory, ignoreCase = true) }
    }

    val currentCatalogList: List<DecorationItem> = remember(selectedTab, selectedRarity) {
        val baseList = when (selectedTab) {
            0 -> ZenkaiDecorationCatalog.AVATAR_OVERLAYS
            3 -> ZenkaiDecorationCatalog.TEXT_EFFECTS
            else -> emptyList()
        }
        if (selectedRarity != null) {
            baseList.filter { it.rarity == selectedRarity }
        } else {
            baseList
        }
    }

    val isPreviewDifferent = (previewOverlayId != userProfile.avatarDecorationId) ||
            (previewTextEffectId != userProfile.textEffectId) ||
            (previewAvatarUrl != userProfile.avatarUrl) ||
            (previewBannerUrl != userProfile.bannerUrl)

    val totalItemsCount = 65 + (if (avatarList.isNotEmpty()) avatarList.size else 658) + (if (bannerList.isNotEmpty()) bannerList.size else 104) + 16

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Header Bar (Pinned)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OttTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Zenkai Locker",
                            color = OttTextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE11D48).copy(alpha = 0.2f))
                                .border(0.5.dp, Color(0xFFE11D48), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$totalItemsCount Items",
                                color = Color(0xFFE11D48),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Animated overlays, avatars, banners & text effects",
                        color = OttTextMuted,
                        fontSize = 11.5.sp
                    )
                }

                if (isPreviewDifferent) {
                    IconButton(
                        onClick = {
                            previewOverlayId = userProfile.avatarDecorationId
                            previewTextEffectId = userProfile.textEffectId
                            previewAvatarUrl = userProfile.avatarUrl
                            previewBannerUrl = userProfile.bannerUrl
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reset Preview",
                            tint = OttTextMuted
                        )
                    }
                }
            }

            // 2. Live Preview Card (Pinned)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(OttCardShape)
                    .background(OttSurface)
                    .border(1.dp, OttSurfaceBorder, OttCardShape)
            ) {
                // Preview Card Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = OttAccentGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "LIVE PROFILE PREVIEW",
                            color = OttAccentGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (isPreviewDifferent) {
                        Text(
                            text = "Unsaved Changes",
                            color = Color(0xFFF59E0B),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Banner Layer (Custom, Preview, or Cinematic Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E2330), OttPanelHigh)
                            )
                        )
                ) {
                    if (previewBannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = previewBannerUrl,
                            contentDescription = "Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Top status shadow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x99000000), Color.Transparent)
                                )
                            )
                    )

                    // Bottom Gradient for avatar blend
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, OttSurface)
                                )
                            )
                    )
                }

                // Avatar + Profile Info Row (Matching ProfileScreen)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .offset(y = (-26).dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AvatarDecorationRenderer(
                        decorationId = previewOverlayId,
                        size = 68.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(OttPanel)
                                .border(2.5.dp, OttTextPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = previewAvatarUrl,
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
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2E3440), OttPanelHigh)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (userProfile.displayName.take(1).ifBlank { "O" }).uppercase(),
                                        color = OttTextPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Display Name with equipped Text Effect + Rank & Badges
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextEffectRenderer(
                                effectId = previewTextEffectId,
                                text = userProfile.displayName.ifBlank { "Otaku" },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (userProfile.isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE11D48))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = OttTextPrimary,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "@${userProfile.username.ifBlank { "otaku" }}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val equippedTag = userProfile.customTag?.takeIf {
                                it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true)
                            }
                            if (equippedTag != null) {
                                LevelRankTagBadge(
                                    tag = equippedTag,
                                    level = userProfile.level
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E2330))
                                        .border(0.5.dp, OttInk(0.20f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = userProfile.otakuRank,
                                        color = OttAccentGold,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(-18.dp))
            }

            // 3. Category Tab Bar (Pinned)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = OttBg,
                contentColor = OttTextPrimary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFE11D48),
                            height = 3.dp
                        )
                    }
                },
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.5.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) OttTextPrimary else OttTextMuted
                            )
                        }
                    )
                }
            }

            // 4. Sub-Filter / Action Bar
            when (selectedTab) {
                0, 3 -> {
                    // Rarity Filter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(rarityFilters) { _, rarity ->
                            val isSelected = selectedRarity == rarity
                            val label = rarity?.label ?: "All Rarities"
                            val chipColor = rarity?.color ?: OttTextPrimary

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) chipColor.copy(alpha = 0.22f) else OttSurface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) chipColor else OttSurfaceBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedRarity = rarity }
                                    .padding(horizontal = 11.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) chipColor else OttTextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Avatar Categories
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(avatarCategories) { _, cat ->
                            val isSelected = selectedAvatarCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFFE11D48).copy(alpha = 0.25f) else OttSurface)
                                    .border(1.dp, if (isSelected) Color(0xFFE11D48) else OttSurfaceBorder, RoundedCornerShape(20.dp))
                                    .clickable { selectedAvatarCategory = cat }
                                    .padding(horizontal = 11.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) OttTextPrimary else OttTextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Banner Categories
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(bannerCategories) { _, cat ->
                            val isSelected = selectedBannerCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFFE11D48).copy(alpha = 0.25f) else OttSurface)
                                    .border(1.dp, if (isSelected) Color(0xFFE11D48) else OttSurfaceBorder, RoundedCornerShape(20.dp))
                                    .clickable { selectedBannerCategory = cat }
                                    .padding(horizontal = 11.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) OttTextPrimary else OttTextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 5. Scrollable Catalog Items List / Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0, 3 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = if (isPreviewDifferent) 100.dp else 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(
                                items = currentCatalogList,
                                key = { index, item -> "${item.id}_${item.type.name}_$index" }
                            ) { _, item ->
                                val isEquipped = when (item.type) {
                                    DecorationType.AVATAR_OVERLAY -> userProfile.avatarDecorationId == item.id
                                    DecorationType.TEXT_EFFECT -> userProfile.textEffectId == item.id
                                    else -> false
                                }

                                val isSelectedInPreview = when (item.type) {
                                    DecorationType.AVATAR_OVERLAY -> previewOverlayId == item.id
                                    DecorationType.TEXT_EFFECT -> previewTextEffectId == item.id
                                    else -> false
                                }

                                DecorationCatalogCard(
                                    item = item,
                                    userDisplayName = userProfile.displayName.ifBlank { "Otaku" },
                                    isEquipped = isEquipped,
                                    isSelectedInPreview = isSelectedInPreview,
                                    onPreviewSelect = {
                                        when (item.type) {
                                            DecorationType.AVATAR_OVERLAY -> previewOverlayId = item.id
                                            DecorationType.TEXT_EFFECT -> previewTextEffectId = item.id
                                            else -> {}
                                        }
                                    },
                                    onEquip = {
                                        when (item.type) {
                                            DecorationType.AVATAR_OVERLAY -> {
                                                previewOverlayId = item.id
                                                vm.equipAvatarDecoration(item.id)
                                            }
                                            DecorationType.TEXT_EFFECT -> {
                                                previewTextEffectId = item.id
                                                vm.equipTextEffect(item.id)
                                            }
                                            else -> {}
                                        }
                                    },
                                    onUnequip = {
                                        when (item.type) {
                                            DecorationType.AVATAR_OVERLAY -> {
                                                previewOverlayId = null
                                                vm.equipAvatarDecoration(null)
                                            }
                                            DecorationType.TEXT_EFFECT -> {
                                                previewTextEffectId = null
                                                vm.equipTextEffect(null)
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                    1 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = if (isPreviewDifferent) 100.dp else 24.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(
                                items = filteredAvatars,
                                key = { _, item -> "avatar_${item.id}" }
                            ) { _, item ->
                                val isEquipped = userProfile.avatarUrl == item.url
                                val isSelectedInPreview = previewAvatarUrl == item.url

                                AvatarGridCard(
                                    item = item,
                                    isEquipped = isEquipped,
                                    isSelectedInPreview = isSelectedInPreview,
                                    onPreviewSelect = {
                                        previewAvatarUrl = item.url
                                    },
                                    onEquip = {
                                        previewAvatarUrl = item.url
                                        vm.updateCloudProfile(avatarUrl = item.url)
                                        Toast.makeText(context, "Avatar equipped to profile!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                    2 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = if (isPreviewDifferent) 100.dp else 24.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(
                                items = filteredBanners,
                                key = { _, item -> "banner_${item.id}" }
                            ) { _, item ->
                                val isEquipped = userProfile.bannerUrl == item.url
                                val isSelectedInPreview = previewBannerUrl == item.url

                                BannerGridCard(
                                    item = item,
                                    isEquipped = isEquipped,
                                    isSelectedInPreview = isSelectedInPreview,
                                    onPreviewSelect = {
                                        previewBannerUrl = item.url
                                    },
                                    onEquip = {
                                        previewBannerUrl = item.url
                                        vm.updateCloudProfile(bannerUrl = item.url)
                                        Toast.makeText(context, "Banner equipped to profile!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Floating Bottom Action Bar (Unsaved Changes)
        AnimatedVisibility(
            visible = isPreviewDifferent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OttPanelHigh)
                    .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Previewing Changes",
                            color = OttTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap Save to equip all to your profile",
                            color = OttTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                previewOverlayId = userProfile.avatarDecorationId
                                previewTextEffectId = userProfile.textEffectId
                                previewAvatarUrl = userProfile.avatarUrl
                                previewBannerUrl = userProfile.bannerUrl
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OttTextSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reset", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (previewOverlayId != userProfile.avatarDecorationId) {
                                    vm.equipAvatarDecoration(previewOverlayId)
                                }
                                if (previewTextEffectId != userProfile.textEffectId) {
                                    vm.equipTextEffect(previewTextEffectId)
                                }
                                if (previewAvatarUrl != userProfile.avatarUrl || previewBannerUrl != userProfile.bannerUrl) {
                                    vm.updateCloudProfile(
                                        avatarUrl = previewAvatarUrl,
                                        bannerUrl = previewBannerUrl
                                    )
                                }
                                Toast.makeText(context, "Saved all profile changes!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecorationCatalogCard(
    item: DecorationItem,
    userDisplayName: String,
    isEquipped: Boolean,
    isSelectedInPreview: Boolean,
    onPreviewSelect: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit
) {
    val borderColor = when {
        isEquipped -> Color(0xFF10B981)
        isSelectedInPreview -> Color(0xFFE11D48)
        else -> OttSurfaceBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(OttCardShape)
            .background(if (isSelectedInPreview) OttSurfaceElevated else OttSurface)
            .border(if (isEquipped || isSelectedInPreview) 1.5.dp else 1.dp, borderColor, OttCardShape)
            .clickable { onPreviewSelect() }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature Visual Preview
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OttPanel)
                        .border(0.5.dp, OttInk(0.20f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (item.type) {
                        DecorationType.AVATAR_OVERLAY -> {
                            AvatarDecorationRenderer(
                                decorationId = item.id,
                                size = 46.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E3440))
                                )
                            }
                        }

                        DecorationType.TEXT_EFFECT -> {
                            TextEffectRenderer(
                                effectId = item.id,
                                text = "Aa",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            color = OttTextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        RarityBadge(rarity = item.rarity)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        color = OttTextMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action Button
                if (isEquipped) {
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.16f))
                                .border(0.5.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Equipped",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remove",
                            color = OttTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { onUnequip() }
                                .padding(4.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onEquip,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelectedInPreview) Color(0xFFE11D48) else Color(0xFF262B3B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Equip",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary
                        )
                    }
                }
            }

            // If it's a Text Effect, show a full live interactive banner on the card itself so the user can preview their name
            if (item.type == DecorationType.TEXT_EFFECT) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(OttPanel)
                        .border(0.5.dp, OttInk(0.13f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    TextEffectRenderer(
                        effectId = item.id,
                        text = userDisplayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarGridCard(
    item: AvatarItem,
    isEquipped: Boolean,
    isSelectedInPreview: Boolean,
    onPreviewSelect: () -> Unit,
    onEquip: () -> Unit
) {
    val borderColor = when {
        isEquipped -> Color(0xFF10B981)
        isSelectedInPreview -> Color(0xFFE11D48)
        else -> OttSurfaceBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelectedInPreview) OttSurfaceElevated else OttSurface)
            .border(if (isEquipped || isSelectedInPreview) 1.5.dp else 0.8.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onPreviewSelect() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar Image with badge
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.url,
                    contentDescription = "Avatar ${item.id}",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(OttPanel)
                        .border(
                            width = if (isSelectedInPreview) 2.dp else 1.dp,
                            color = if (isSelectedInPreview) Color(0xFFE11D48) else OttInk(0.20f),
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )

                if (isEquipped) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Equipped",
                            tint = OttTextPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tag or ID
            Text(
                text = item.tags.firstOrNull() ?: "#${item.id}",
                color = if (isSelectedInPreview) OttTextPrimary else OttTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Equip button
            if (isEquipped) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Set",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelectedInPreview) Color(0xFFE11D48) else Color(0xFF262B3B))
                        .clickable { onEquip() }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Use",
                        color = OttTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerGridCard(
    item: BannerItem,
    isEquipped: Boolean,
    isSelectedInPreview: Boolean,
    onPreviewSelect: () -> Unit,
    onEquip: () -> Unit
) {
    val borderColor = when {
        isEquipped -> Color(0xFF10B981)
        isSelectedInPreview -> Color(0xFFE11D48)
        else -> OttSurfaceBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelectedInPreview) OttSurfaceElevated else OttSurface)
            .border(if (isEquipped || isSelectedInPreview) 1.5.dp else 0.8.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onPreviewSelect() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Banner 16:9 box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(OttPanelHigh)
            ) {
                AsyncImage(
                    model = item.url,
                    contentDescription = "Banner ${item.id}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Equipped badge on top left
                if (isEquipped) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EQUIPPED",
                            color = OttTextPrimary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Bottom bar with category and equip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category.ifBlank { "General" },
                    color = OttTextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (isEquipped) {
                    Text(
                        text = "Active",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelectedInPreview) Color(0xFFE11D48) else Color(0xFF262B3B))
                            .clickable { onEquip() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Equip",
                            color = OttTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

