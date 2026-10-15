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
fun BannerPickerScreen(
    currentBannerUrl: String,
    onBannerSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedUrl by remember { mutableStateOf(currentBannerUrl) }
    var selectedCategory by remember { mutableStateOf("All") }
    var customUrl by remember { mutableStateOf("") }
    
    val bannersList by AvatarBannerRepository.bannersFlow.collectAsState()
    val allBanners = if (bannersList.isNotEmpty()) bannersList else remember(context) { AvatarBannerRepository.getBanners(context) }

    val categories = remember(allBanners) {
        val cats = mutableSetOf<String>()
        allBanners.forEach { item ->
            if (item.category.isNotBlank()) {
                cats.add(item.category)
            }
        }
        listOf("All") + cats.sorted()
    }

    val filteredBanners = remember(selectedCategory, allBanners) {
        if (selectedCategory == "All") {
            allBanners
        } else {
            allBanners.filter { it.category.equals(selectedCategory, ignoreCase = true) }
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
                text = "Choose Banner",
                color = OttTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${filteredBanners.size} banners",
                color = OttTextMuted,
                fontSize = 13.sp
            )
        }

        // Current Banner Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            AsyncImage(
                model = selectedUrl.ifEmpty { allBanners.firstOrNull()?.url ?: "" },
                contentDescription = "Current Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(OttCardShape)
                    .border(2.dp, OttPrimary, OttCardShape),
                contentScale = ContentScale.Crop
            )
        }

        // Category Filter Tabs
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

        // Banner Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredBanners, key = { it.id }) { banner ->
                val isSelected = selectedUrl == banner.url
                Box(
                    modifier = Modifier
                        .aspectRatio(16f/9f)
                        .clip(OttCardShape)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) OttPrimary else OttSurfaceBorder,
                            shape = OttCardShape
                        )
                        .clickable { 
                            selectedUrl = banner.url
                            customUrl = ""
                        }
                ) {
                    AsyncImage(
                        model = banner.url,
                        contentDescription = "Banner Option",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(OttPrimary.copy(alpha = 0.25f))
                        )
                    }
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
            onClick = { onBannerSelected(selectedUrl) },
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
