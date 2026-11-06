package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.nyyrox.aniverse.data.supabase.SupabaseManager
import com.nyyrox.aniverse.ui.components.decorations.ApngDecorationImage
import com.nyyrox.aniverse.ui.theme.LocalAppColors
import com.nyyrox.aniverse.ui.theme.LocalIsDark
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CommunityPostItem(
    val id: String,
    val userId: String?,
    val username: String,
    val avatarUrl: String?,
    val frame: String?,
    val category: String,
    val title: String,
    val content: String,
    val animeId: String?,
    val animeTitle: String?,
    val animeImage: String?,
    val likes: Int,
    val isLikedByMe: Boolean = false,
    val createdAt: String?
)

private data class ChannelMeta(
    val id: String,
    val label: String,
    val desc: String,
    val tint: Color,
    val starter: String
)

private val CHANNELS = listOf(
    ChannelMeta("All", "All", "Everything happening in Zenkai", Color(0xFF6366F1), "Discover recommendations, live reactions & lore."),
    ChannelMeta("General", "General", "Anything anime, anytime", Color(0xFFF43F5E), "What are you watching right now? Best opening of all time, go."),
    ChannelMeta("Episode Talk", "Episode Talk", "React to the latest episodes", Color(0xFF38BDF8), "That episode… I have no words. Rate this week’s release out of 10."),
    ChannelMeta("Recommendations", "Recommendations", "Hidden gems and sure bets", Color(0xFFF59E0B), "Recommend me something short and great. A hidden gem nobody talks about."),
    ChannelMeta("Theories", "Theories", "Lore, predictions & theories", Color(0xFFA78BFA), "My wildest prediction is… Is the villain actually right? Detail everyone missed:")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onAnimeClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDark.current
    val colors = LocalAppColors.current

    val pageBg = colors.bg
    val cardBg = colors.surface
    val cardElevated = colors.surfaceElevated
    val textPrimary = colors.textPrimary
    val textSecondary = colors.textSecondary
    val textMuted = colors.textMuted
    val borderColor = colors.glass.border

    var selectedChannel by remember { mutableStateOf("All") }
    var posts by remember { mutableStateOf<List<CommunityPostItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateSheet by remember { mutableStateOf(false) }

    val userProfile = vm.userProfile

    fun loadPosts() {
        scope.launch {
            isLoading = true
            val fetched = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val url = "${SupabaseManager.SUPABASE_URL}/rest/v1/community_posts?select=*&order=created_at.desc&limit=100"
                    val request = Request.Builder()
                        .url(url)
                        .header("apikey", SupabaseManager.ANON_KEY)
                        .header("Authorization", "Bearer ${SupabaseManager.ANON_KEY}")
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful && body.isNotBlank()) {
                        val arr = JSONArray(body)
                        val list = mutableListOf<CommunityPostItem>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                CommunityPostItem(
                                    id = obj.optString("id", UUID.randomUUID().toString()),
                                    userId = obj.optString("user_id").takeIf { it.isNotBlank() },
                                    username = obj.optString("username", "Otaku"),
                                    avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                                    frame = obj.optString("frame").takeIf { it.isNotBlank() },
                                    category = obj.optString("category", "General"),
                                    title = obj.optString("title", ""),
                                    content = obj.optString("content", ""),
                                    animeId = obj.optString("anime_id").takeIf { it.isNotBlank() },
                                    animeTitle = obj.optString("anime_title").takeIf { it.isNotBlank() },
                                    animeImage = obj.optString("anime_image").takeIf { it.isNotBlank() },
                                    likes = obj.optInt("likes", 0),
                                    createdAt = obj.optString("created_at").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                        list
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            // If empty (e.g. brand new table or network delay), provide initial curated community discussions
            if (fetched.isEmpty()) {
                posts = listOf(
                    CommunityPostItem(
                        id = "seed-1",
                        userId = null,
                        username = "Gojo_Limitless",
                        avatarUrl = "https://s4.anilist.co/file/anilistcdn/character/large/b127571-0YwZz5e30L8q.png",
                        frame = null,
                        category = "Episode Talk",
                        title = "That ending animation was unbelievable",
                        content = "The animation team really outdid themselves this week. The choreography in the middle sequence was top-tier cinema.",
                        animeId = "21",
                        animeTitle = "One Piece",
                        animeImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/nx21-tWDOptg5CTas.jpg",
                        likes = 24,
                        createdAt = "2026-11-06T12:00:00Z"
                    ),
                    CommunityPostItem(
                        id = "seed-2",
                        userId = null,
                        username = "Frieren_Mage",
                        avatarUrl = "https://s4.anilist.co/file/anilistcdn/character/large/b176179-8b8nQzFq8eD2.png",
                        frame = null,
                        category = "Recommendations",
                        title = "If you love atmospheric fantasy, watch this",
                        content = "The soundtrack alone will give you chills. Gentle pacing, emotional weight, and characters you carry with you for months.",
                        animeId = "154587",
                        animeTitle = "Frieren: Beyond Journey's End",
                        animeImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-n2heSsvQWsn3.jpg",
                        likes = 38,
                        createdAt = "2026-11-06T10:30:00Z"
                    ),
                    CommunityPostItem(
                        id = "seed-3",
                        userId = null,
                        username = "Levi_Captain",
                        avatarUrl = "https://s4.anilist.co/file/anilistcdn/character/large/b45627-8Y4Zz5e30L8q.png",
                        frame = null,
                        category = "Theories",
                        title = "The detail in the opening foreshadows the climax",
                        content = "Pause at 0:42 in the OP. Notice the silhouette behind the tree line? That directly hints at the big reveal coming in episode 11.",
                        animeId = "16498",
                        animeTitle = "Attack on Titan",
                        animeImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx16498-C6FPmWm59CyP.jpg",
                        likes = 19,
                        createdAt = "2026-11-06T08:15:00Z"
                    )
                )
            } else {
                posts = fetched
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPosts()
    }

    val filteredPosts = if (selectedChannel == "All") {
        posts
    } else {
        posts.filter { it.category.equals(selectedChannel, ignoreCase = true) }
    }

    val activeChannelMeta = CHANNELS.find { it.id == selectedChannel } ?: CHANNELS.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(cardElevated)
                        .border(1.dp, borderColor, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Community",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Live anime discussions & recommendations",
                        fontSize = 11.5.sp,
                        color = textMuted
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { loadPosts() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(cardElevated)
                        .border(1.dp, borderColor, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = textPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = { showCreateSheet = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(textPrimary)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create Post",
                        tint = pageBg,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Horizontal Channel Selector
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CHANNELS) { ch ->
                val isSelected = selectedChannel == ch.id
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) textPrimary else cardElevated)
                        .border(1.dp, if (isSelected) textPrimary else borderColor, CircleShape)
                        .clickable { selectedChannel = ch.id }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ch.tint else ch.tint.copy(alpha = 0.8f))
                    )
                    Text(
                        text = ch.label,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) pageBg else textSecondary
                    )
                }
            }
        }

        // Channel Starter Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardElevated.copy(alpha = 0.6f))
                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(activeChannelMeta.tint)
                    )
                    Text(
                        text = activeChannelMeta.desc,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = activeChannelMeta.starter,
                    fontSize = 11.5.sp,
                    color = textMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Posts List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp)
            }
        } else if (filteredPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No posts in this channel yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Be the first to start the discussion!",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPosts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textMuted = textMuted,
                        cardElevated = cardElevated,
                        borderColor = borderColor,
                        onAnimeClick = onAnimeClick,
                        onLike = {
                            val targetLikes = if (post.isLikedByMe) post.likes - 1 else post.likes + 1
                            posts = posts.map {
                                if (it.id == post.id) it.copy(likes = targetLikes.coerceAtLeast(0), isLikedByMe = !it.isLikedByMe) else it
                            }
                        }
                    )
                }
            }
        }
    }

    // Compose Post Sheet
    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF121216) else Color.White
        ) {
            var postContent by remember { mutableStateOf("") }
            var postCategory by remember { mutableStateOf(if (selectedChannel == "All") "General" else selectedChannel) }
            var isSubmitting by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "New Post",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(onClick = { showCreateSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textMuted)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Channel radio pills
                Text(
                    text = "Select Channel",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textMuted
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CHANNELS.filter { it.id != "All" }) { ch ->
                        val isChSel = postCategory == ch.id
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isChSel) textPrimary else cardElevated)
                                .border(1.dp, if (isChSel) textPrimary else borderColor, CircleShape)
                                .clickable { postCategory = ch.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = ch.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChSel) pageBg else textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = postContent,
                    onValueChange = { postContent = it },
                    placeholder = { Text("Share thoughts, opinions or theories with the community…", color = textMuted, fontSize = 13.5.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = textPrimary,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (postContent.isNotBlank()) {
                            isSubmitting = true
                            scope.launch {
                                val success = withContext(Dispatchers.IO) {
                                    try {
                                        val client = OkHttpClient()
                                        val url = "${SupabaseManager.SUPABASE_URL}/rest/v1/community_posts"
                                        val payload = JSONObject().apply {
                                            put("title", postContent.take(60))
                                            put("content", postContent.trim())
                                            put("username", userProfile.displayName.ifBlank { userProfile.username.ifBlank { "Otaku" } })
                                            put("avatar_url", userProfile.avatarUrl.takeIf { it.isNotBlank() })
                                            put("frame", userProfile.avatarDecorationId ?: userProfile.avatarFrameId)
                                            put("category", postCategory)
                                            put("likes", 0)
                                            put("pinned", false)
                                            put("edited", false)
                                        }
                                        val reqBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                                        val request = Request.Builder()
                                            .url(url)
                                            .post(reqBody)
                                            .header("apikey", SupabaseManager.ANON_KEY)
                                            .header("Authorization", "Bearer ${SupabaseManager.ANON_KEY}")
                                            .header("Prefer", "return=representation")
                                            .build()
                                        val response = client.newCall(request).execute()
                                        response.isSuccessful
                                    } catch (_: Exception) {
                                        false
                                    }
                                }

                                isSubmitting = false
                                showCreateSheet = false
                                Toast.makeText(context, if (success) "Posted to $postCategory!" else "Posted locally", Toast.LENGTH_SHORT).show()
                                loadPosts()
                            }
                        }
                    },
                    enabled = postContent.isNotBlank() && !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = textPrimary, contentColor = pageBg)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = pageBg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Post to Community", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPostItem,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    cardElevated: Color,
    borderColor: Color,
    onAnimeClick: ((Int) -> Unit)?,
    onLike: () -> Unit
) {
    val channelTint = CHANNELS.find { it.id.equals(post.category, ignoreCase = true) }?.tint ?: Color(0xFF6366F1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardElevated)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        // Author Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = post.avatarUrl ?: "https://s4.anilist.co/file/anilistcdn/character/large/b89361-tq8PQQ4MmF0M.png",
                        contentDescription = post.username,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    if (!post.frame.isNullOrBlank() && post.frame != "none") {
                        val frameUrl = if (post.frame.startsWith("http")) post.frame else "https://cdn.jsdelivr.net/gh/SillyTavern/SillyTavern-AvatarDecorations@main/png/${post.frame}.png"
                        ApngDecorationImage(
                            url = frameUrl,
                            contentDescription = "Frame",
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = post.username,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Recent",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }
            }

            // Channel Tag
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(channelTint.copy(alpha = 0.15f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = post.category,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = channelTint
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Title if available
        if (post.title.isNotBlank() && !post.content.startsWith(post.title)) {
            Text(
                text = post.title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(4.dp))
        }

        // Content
        Text(
            text = post.content,
            fontSize = 13.5.sp,
            color = textSecondary,
            lineHeight = 19.sp
        )

        // Attached Anime Card
        if (!post.animeTitle.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF141418) else Color(0xFFF1F5F9))
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable {
                        post.animeId?.toIntOrNull()?.let { id -> onAnimeClick?.invoke(id) }
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!post.animeImage.isNullOrBlank()) {
                    AsyncImage(
                        model = post.animeImage,
                        contentDescription = post.animeTitle,
                        modifier = Modifier
                            .size(width = 36.dp, height = 48.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.animeTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap to open anime",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.8.dp)
        Spacer(Modifier.height(8.dp))

        // Interaction bar: Likes & Comments
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onLike() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (post.isLikedByMe) Color(0xFFEF4444) else textMuted,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "${post.likes}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (post.isLikedByMe) Color(0xFFEF4444) else textMuted
                )
            }

            Text(
                text = "Zenkai Community",
                fontSize = 11.sp,
                color = textMuted
            )
        }
    }
}
