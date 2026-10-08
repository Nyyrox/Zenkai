package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.AvatarBannerRepository
import com.nyyrox.aniverse.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerScreen(
    currentAvatarUrl: String,
    onAvatarSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedUrl by remember { mutableStateOf(currentAvatarUrl) }
    var selectedCategory by remember { mutableStateOf("All") }
    var customUrl by remember { mutableStateOf("") }
    
    val avatarsList by AvatarBannerRepository.avatarsFlow.collectAsState()
    val allAvatars = if (avatarsList.isNotEmpty()) avatarsList else remember(context) { AvatarBannerRepository.getAvatars(context) }

    val categories = remember(allAvatars) {
        val tags = mutableSetOf<String>()
        allAvatars.forEach { item ->
            tags.addAll(item.tags)
        }
        listOf("All") + tags.filter { it.isNotBlank() }.sorted()
    }

    val filteredAvatars = remember(selectedCategory, allAvatars) {
        if (selectedCategory == "All") {
            allAvatars
        } else {
            allAvatars.filter { item -> item.tags.any { it.equals(selectedCategory, ignoreCase = true) } }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(OttSurface, CircleShape)
                    .border(1.dp, OttSurfaceBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OttTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Choose Avatar",
                color = OttTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${filteredAvatars.size} avatars",
                color = OttTextMuted,
                fontSize = 13.sp
            )
        }

        // Current Avatar Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = selectedUrl.ifEmpty { "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Fallback" },
                contentDescription = "Current Avatar",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, OttPrimary, CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Categories
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) OttPrimary.copy(alpha = 0.2f) else OttSurface,
                            OttPillShape
                        )
                        .border(
                            1.dp,
                            if (isSelected) OttPrimary else OttSurfaceBorder,
                            OttPillShape
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) OttPrimary else OttTextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Avatar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredAvatars, key = { it.id }) { avatar ->
                val isSelected = selectedUrl == avatar.url
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(OttSurface, OttCardShape)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) OttPrimary else OttSurfaceBorder,
                            shape = OttCardShape
                        )
                        .clickable { 
                            selectedUrl = avatar.url
                            customUrl = ""
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = avatar.url,
                        contentDescription = "Avatar Option",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Custom URL Input
        OutlinedTextField(
            value = customUrl,
            onValueChange = { 
                customUrl = it
                if (it.isNotEmpty()) {
                    selectedUrl = it
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Or paste a custom URL", color = OttTextMuted, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = OttSurface,
                unfocusedContainerColor = OttSurface,
                focusedBorderColor = OttPrimary,
                unfocusedBorderColor = OttSurfaceBorder,
                focusedTextColor = OttTextPrimary,
                unfocusedTextColor = OttTextPrimary
            ),
            shape = OttCardShape,
            singleLine = true
        )

        // Save Button
        Button(
            onClick = { onAvatarSelected(selectedUrl) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(OttPrimary, OttSecondary)
                    ),
                    shape = OttPillShape
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues()
        ) {
            Text(
                text = "Save",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
