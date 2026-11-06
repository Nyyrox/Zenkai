package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.supabase.SupabaseManager
import com.nyyrox.aniverse.ui.components.decorations.ApngDecorationImage
import com.nyyrox.aniverse.ui.theme.LocalAppColors
import com.nyyrox.aniverse.ui.theme.LocalIsDark
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/* ------------------------------------------------------------------ Data Models */

data class CommunityMessage(
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
    val pinned: Boolean = false,
    val edited: Boolean = false,
    val createdAt: String
)

data class CommunityChannel(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val desc: String,
    val tint: Color,
    val starters: List<String>
)

val CHANNELS = listOf(
    CommunityChannel(
        id = "General",
        label = "general",
        icon = Icons.Default.Forum,
        desc = "Anything anime, anytime",
        tint = Color(0xFFF43F5E),
        starters = listOf(
            "What are you watching right now?",
            "Which anime made you cry the hardest?",
            "Best opening of all time, go.",
            "What anime did you drop and why?"
        )
    ),
    CommunityChannel(
        id = "Episode Talk",
        label = "episode-talk",
        icon = Icons.Default.Tv,
        desc = "React to the latest episodes",
        tint = Color(0xFF38BDF8),
        starters = listOf(
            "That episode… I have no words.",
            "Rate this week’s episode out of 10.",
            "Animation of the season so far?",
            "Who stole the show this week?"
        )
    ),
    CommunityChannel(
        id = "Recommendations",
        label = "recommendations",
        icon = Icons.Default.Whatshot,
        desc = "Hidden gems and sure bets",
        tint = Color(0xFFF59E0B),
        starters = listOf(
            "Recommend me something short and great.",
            "A hidden gem nobody talks about:",
            "Best anime to start with for a newbie?",
            "Something that feels like Frieren?"
        )
    ),
    CommunityChannel(
        id = "Theories",
        label = "theories",
        icon = Icons.Default.AutoAwesome,
        desc = "Lore, predictions, spoilers",
        tint = Color(0xFFA78BFA),
        starters = listOf(
            "My wildest prediction is…",
            "Is the villain actually right?",
            "A detail everyone missed:",
            "How do you think it ends?"
        )
    )
)

private val EMOJIS = listOf("🔥", "❤️", "✨", "🍿", "😂", "😭", "💯", "👑", "🫡")
private const val MAX_MSG_LEN = 600

private sealed class ChatRowItem {
    data class DayHeader(val key: String, val label: String) : ChatRowItem()
    data class Message(
        val key: String,
        val msg: CommunityMessage,
        val first: Boolean,
        val last: Boolean
    ) : ChatRowItem()
}

/* ------------------------------------------------------------------ Helpers */

private fun parseIsoDate(iso: String?): Date? {
    if (iso.isNullOrBlank()) return null
    return try {
        val clean = iso.substringBefore(".").substringBefore("Z").substringBefore("+")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        sdf.parse(clean)
    } catch (_: Exception) {
        null
    }
}

private fun formatTime(iso: String): String {
    val date = parseIsoDate(iso) ?: return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

private fun dayLabel(iso: String): String {
    val date = parseIsoDate(iso) ?: return "Today"
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { time = date }
    if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
    ) {
        return "Today"
    }
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
    ) {
        return "Yesterday"
    }
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
    return fmt.format(date)
}

private fun joins(a: CommunityMessage?, b: CommunityMessage?): Boolean {
    if (a == null || b == null || a.username != b.username) return false
    val ta = parseIsoDate(a.createdAt) ?: return false
    val tb = parseIsoDate(b.createdAt) ?: return false
    val calA = Calendar.getInstance().apply { time = ta }
    val calB = Calendar.getInstance().apply { time = tb }
    val sameDay = calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    val diffMs = Math.abs(tb.time - ta.time)
    return sameDay && diffMs < 5 * 60 * 1000
}

private fun buildRows(messages: List<CommunityMessage>): List<ChatRowItem> {
    val rows = mutableListOf<ChatRowItem>()
    messages.forEachIndexed { i, msg ->
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)

        val prevDate = prev?.let { parseIsoDate(it.createdAt) }
        val curDate = parseIsoDate(msg.createdAt)
        val isNewDay = if (prevDate != null && curDate != null) {
            val c1 = Calendar.getInstance().apply { time = prevDate }
            val c2 = Calendar.getInstance().apply { time = curDate }
            c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR) ||
                    c1.get(Calendar.DAY_OF_YEAR) != c2.get(Calendar.DAY_OF_YEAR)
        } else {
            prev == null
        }

        if (isNewDay) {
            rows.add(ChatRowItem.DayHeader("day-${msg.id}", dayLabel(msg.createdAt)))
        }

        val first = !joins(prev, msg)
        val last = !joins(msg, next)
        rows.add(ChatRowItem.Message("msg-${msg.id}", msg, first = first, last = last))
    }
    return rows
}

/* ------------------------------------------------------------------ CommunityScreen */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onAnimeClick: ((Int) -> Unit)? = null,
    onOpenUserProfile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDark.current
    val colors = LocalAppColors.current

    val pageBg = colors.bg
    val cardElevated = colors.surfaceElevated
    val textPrimary = colors.textPrimary
    val textSecondary = colors.textSecondary
    val textMuted = colors.textMuted
    val borderColor = colors.glass.border
    val accent = colors.accent

    val userProfile = vm.userProfile
    val myUsername = userProfile.displayName.ifBlank { userProfile.username.ifBlank { "Otaku" } }
    val myAvatar = userProfile.avatarUrl.takeIf { it.isNotBlank() }
    val myFrame = userProfile.avatarDecorationId ?: userProfile.avatarFrameId

    var selectedChannelId by remember { mutableStateOf("General") }
    val activeChannel = CHANNELS.find { it.id == selectedChannelId } ?: CHANNELS.first()

    var messages by remember { mutableStateOf<List<CommunityMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Tagged anime for the current draft
    var taggedAnime by remember { mutableStateOf<Anime?>(null) }
    var showTagSheet by remember { mutableStateOf(false) }

    // Room pulse bottom sheet
    var showPulseSheet by remember { mutableStateOf(false) }

    // Edit message dialog / sheet
    var editingMessage by remember { mutableStateOf<CommunityMessage?>(null) }
    var editTextValue by remember { mutableStateOf("") }
    var isSavingEdit by remember { mutableStateOf(false) }

    // Liked post IDs in memory
    val likedMsgIds = remember { mutableStateMapOf<String, Boolean>() }

    // Spoilers revealed set (messageId:index)
    val revealedSpoilers = remember { mutableStateMapOf<String, Boolean>() }

    val listState = rememberLazyListState()

    // Load messages from Supabase
    suspend fun fetchPosts(channelId: String): List<CommunityMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val url = "${SupabaseManager.SUPABASE_URL}/rest/v1/community_posts?category=eq.${Uri.encode(channelId)}&select=*&order=created_at.asc&limit=150"
                val request = Request.Builder()
                    .url(url)
                    .header("apikey", SupabaseManager.ANON_KEY)
                    .header("Authorization", "Bearer ${SupabaseManager.ANON_KEY}")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val arr = JSONArray(body)
                    val list = mutableListOf<CommunityMessage>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            CommunityMessage(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                userId = obj.optString("user_id").takeIf { it.isNotBlank() },
                                username = obj.optString("username", "Otaku"),
                                avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                                frame = obj.optString("frame").takeIf { it.isNotBlank() },
                                category = obj.optString("category", channelId),
                                title = obj.optString("title", ""),
                                content = obj.optString("content", ""),
                                animeId = obj.optString("anime_id").takeIf { it.isNotBlank() },
                                animeTitle = obj.optString("anime_title").takeIf { it.isNotBlank() },
                                animeImage = obj.optString("anime_image").takeIf { it.isNotBlank() },
                                likes = obj.optInt("likes", 0),
                                isLikedByMe = likedMsgIds[obj.optString("id")] == true,
                                pinned = obj.optBoolean("pinned", false),
                                edited = obj.optBoolean("edited", false),
                                createdAt = obj.optString("created_at", "")
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
    }

    // Polling effect every 4s
    LaunchedEffect(selectedChannelId) {
        isLoading = true
        val initial = fetchPosts(selectedChannelId)
        messages = initial
        isLoading = false
        if (initial.isNotEmpty()) {
            delay(80)
            listState.scrollToItem(initial.size - 1)
        }

        while (isActive) {
            delay(4000)
            val updated = fetchPosts(selectedChannelId)
            if (updated.isNotEmpty()) {
                val wasAtBottom = !listState.canScrollForward
                messages = updated
                if (wasAtBottom) {
                    listState.animateScrollToItem(updated.size - 1)
                }
            }
        }
    }

    // Like message action via RPC
    fun onLikePost(msg: CommunityMessage) {
        if (likedMsgIds[msg.id] == true) return
        likedMsgIds[msg.id] = true
        messages = messages.map {
            if (it.id == msg.id) it.copy(likes = it.likes + 1, isLikedByMe = true) else it
        }

        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val url = "${SupabaseManager.SUPABASE_URL}/rest/v1/rpc/like_community_post"
                val payload = JSONObject().apply {
                    put("target_post_id", msg.id)
                }
                val reqBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .header("apikey", SupabaseManager.ANON_KEY)
                    .header("Authorization", "Bearer ${SupabaseManager.ANON_KEY}")
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
        }
    }

    // Send message action
    fun sendMessage() {
        val content = inputText.trim()
        if (content.isBlank() || isSending) return
        isSending = true

        val currentTag = taggedAnime
        val tempId = "temp-${System.currentTimeMillis()}"
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(Date())

        val optimisticMsg = CommunityMessage(
            id = tempId,
            userId = userProfile.username.ifBlank { null },
            username = myUsername,
            avatarUrl = myAvatar,
            frame = myFrame,
            category = selectedChannelId,
            title = content.take(60),
            content = content,
            animeId = currentTag?.id?.toString(),
            animeTitle = currentTag?.title,
            animeImage = currentTag?.cover,
            likes = 0,
            isLikedByMe = false,
            pinned = false,
            edited = false,
            createdAt = nowIso
        )

        messages = messages + optimisticMsg
        inputText = ""
        taggedAnime = null

        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val success = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val url = "${SupabaseManager.SUPABASE_URL}/rest/v1/community_posts"
                    val payload = JSONObject().apply {
                        put("title", optimisticMsg.title)
                        put("content", content)
                        put("username", myUsername)
                        put("avatar_url", myAvatar ?: JSONObject.NULL)
                        put("frame", myFrame ?: JSONObject.NULL)
                        put("category", selectedChannelId)
                        put("anime_title", currentTag?.title ?: JSONObject.NULL)
                        put("anime_id", currentTag?.id?.toString() ?: JSONObject.NULL)
                        put("anime_image", currentTag?.cover ?: JSONObject.NULL)
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
            isSending = false
            if (!success) {
                Toast.makeText(context, "Could not send message", Toast.LENGTH_SHORT).show()
                messages = messages.filter { it.id != tempId }
                inputText = content
            } else {
                // Refresh to get server timestamps
                val refreshed = fetchPosts(selectedChannelId)
                if (refreshed.isNotEmpty()) messages = refreshed
            }
        }
    }

    // Delete post action
    fun deletePost(msg: CommunityMessage) {
        messages = messages.filter { it.id != msg.id }
        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val url = "${SupabaseManager.SUPABASE_URL}/rest/v1/community_posts?id=eq.${msg.id}"
                val request = Request.Builder()
                    .url(url)
                    .delete()
                    .header("apikey", SupabaseManager.ANON_KEY)
                    .header("Authorization", "Bearer ${SupabaseManager.ANON_KEY}")
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
        }
    }

    // Save edited post
    fun saveEdit(msg: CommunityMessage, newContent: String) {
        val trimmed = newContent.trim()
        if (trimmed.isBlank() || isSavingEdit) return
        isSavingEdit = true
        messages = messages.map {
            if (it.id == msg.id) it.copy(content = trimmed, edited = true) else it
        }
        editingMessage = null

        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val url = "${SupabaseManager.SUPABASE_URL}/rest/v1/community_posts?id=eq.${msg.id}"
                val payload = JSONObject().apply {
                    put("content", trimmed)
                    put("edited", true)
                }
                val reqBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(url)
                    .patch(reqBody)
                    .header("apikey", SupabaseManager.ANON_KEY)
                    .header("Authorization", "Bearer ${SupabaseManager.ANON_KEY}")
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
            isSavingEdit = false
        }
    }

    val rows = remember(messages) { buildRows(messages) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Ambient Radial Glow from Top matching Active Channel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            activeChannel.tint.copy(alpha = 0.18f),
                            activeChannel.tint.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardElevated)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(activeChannel.tint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeChannel.icon,
                            contentDescription = null,
                            tint = activeChannel.tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "#${activeChannel.label}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Text(
                            text = activeChannel.desc,
                            fontSize = 11.sp,
                            color = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Room pulse button
                    IconButton(
                        onClick = { showPulseSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardElevated)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = "Room pulse",
                            tint = activeChannel.tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Refresh button
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                messages = fetchPosts(selectedChannelId)
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
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
                }
            }

            // Channel Selector Horizontal Pills
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CHANNELS) { ch ->
                    val isSel = ch.id == selectedChannelId
                    val bgCol by animateColorAsState(if (isSel) ch.tint else cardElevated, label = "bgCol")
                    val textCol by animateColorAsState(if (isSel) Color.White else textMuted, label = "textCol")

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(bgCol)
                            .border(1.dp, if (isSel) ch.tint else borderColor, CircleShape)
                            .clickable { selectedChannelId = ch.id }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ch.icon,
                            contentDescription = null,
                            tint = textCol,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "#${ch.label}",
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = textCol
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = borderColor.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )

            // Message Stream
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading && messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = activeChannel.tint, strokeWidth = 2.dp)
                    }
                } else if (messages.isEmpty()) {
                    // Empty state matching web lounge
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(activeChannel.tint.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeChannel.icon,
                                contentDescription = null,
                                tint = activeChannel.tint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "#${activeChannel.label} is waiting for you",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Start the conversation. Tap a starter below or write your own.",
                            fontSize = 12.sp,
                            color = textMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))

                        activeChannel.starters.forEach { starter ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardElevated)
                                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable { inputText = starter }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = starter,
                                    fontSize = 12.5.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(rows, key = {
                            when (it) {
                                is ChatRowItem.DayHeader -> it.key
                                is ChatRowItem.Message -> it.key
                            }
                        }) { item ->
                            when (item) {
                                is ChatRowItem.DayHeader -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = borderColor, thickness = 0.8.dp)
                                        Text(
                                            text = item.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textMuted,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = borderColor, thickness = 0.8.dp)
                                    }
                                }

                                is ChatRowItem.Message -> {
                                    val msg = item.msg
                                    val isMe = msg.username == myUsername || (msg.userId != null && msg.userId == userProfile.username)
                                    val bubbleShape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (!isMe && item.last) 4.dp else 16.dp,
                                        bottomEnd = if (isMe && item.last) 4.dp else 16.dp
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = if (item.first) 6.dp else 1.dp),
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                    ) {
                                        // Sender Header (only on first message of cluster)
                                        if (item.first) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isMe) "${msg.username} (You)" else msg.username,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMe) accent else textPrimary,
                                                    modifier = Modifier.clickable {
                                                        onOpenUserProfile?.invoke(msg.username)
                                                    }
                                                )
                                                Text(
                                                    text = formatTime(msg.createdAt),
                                                    fontSize = 10.sp,
                                                    color = textMuted
                                                )
                                                if (msg.edited) {
                                                    Text(
                                                        text = "(edited)",
                                                        fontSize = 9.5.sp,
                                                        color = textMuted.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }

                                        // Bubble Row with Avatar on Last
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (!isMe) {
                                                if (item.last) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clickable { onOpenUserProfile?.invoke(msg.username) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        SubcomposeAsyncImage(
                                                            model = msg.avatarUrl ?: "https://s4.anilist.co/file/anilistcdn/character/large/b89361-tq8PQQ4MmF0M.png",
                                                            contentDescription = msg.username,
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        if (!msg.frame.isNullOrBlank() && msg.frame != "none") {
                                                            val frameUrl = if (msg.frame.startsWith("http")) msg.frame else "https://cdn.jsdelivr.net/gh/SillyTavern/SillyTavern-AvatarDecorations@main/png/${msg.frame}.png"
                                                            ApngDecorationImage(
                                                                url = frameUrl,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(34.dp)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Spacer(Modifier.width(34.dp))
                                                }
                                                Spacer(Modifier.width(6.dp))
                                            }

                                            // Main Message Bubble
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.82f)
                                                    .clip(bubbleShape)
                                                    .background(
                                                        if (isMe) {
                                                            Brush.linearGradient(
                                                                listOf(
                                                                    accent.copy(alpha = 0.95f),
                                                                    accent.copy(alpha = 0.78f)
                                                                )
                                                            )
                                                        } else {
                                                            SolidColor(cardElevated)
                                                        }
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isMe) accent.copy(alpha = 0.4f) else borderColor,
                                                        bubbleShape
                                                    )
                                                    .combinedClickable(
                                                        onClick = {},
                                                        onDoubleClick = { onLikePost(msg) }
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                                            ) {
                                                // Attached Show Card
                                                if (!msg.animeTitle.isNullOrBlank()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 6.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.Black.copy(alpha = 0.35f))
                                                            .clickable {
                                                                msg.animeId?.toIntOrNull()?.let { id ->
                                                                    onAnimeClick?.invoke(id)
                                                                }
                                                            }
                                                            .padding(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (!msg.animeImage.isNullOrBlank()) {
                                                            AsyncImage(
                                                                model = msg.animeImage,
                                                                contentDescription = msg.animeTitle,
                                                                modifier = Modifier
                                                                    .size(width = 32.dp, height = 44.dp)
                                                                    .clip(RoundedCornerShape(4.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = if (msg.category == "Recommendations") "Recommended" else "Talking about",
                                                                fontSize = 9.5.sp,
                                                                color = Color.White.copy(alpha = 0.75f)
                                                            )
                                                            Text(
                                                                text = msg.animeTitle,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.PlayArrow,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(10.dp)
                                                                )
                                                                Text(
                                                                    text = "Watch on Zenkai",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Message Text with Spoiler Parsing
                                                CommunityMessageRichText(
                                                    content = msg.content,
                                                    msgId = msg.id,
                                                    isMe = isMe,
                                                    revealedSpoilers = revealedSpoilers,
                                                    textColor = if (isMe) Color.White else textPrimary
                                                )
                                            }

                                            if (isMe) {
                                                Spacer(Modifier.width(6.dp))
                                                if (item.last) {
                                                    Box(
                                                        modifier = Modifier.size(34.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        SubcomposeAsyncImage(
                                                            model = myAvatar ?: "https://s4.anilist.co/file/anilistcdn/character/large/b89361-tq8PQQ4MmF0M.png",
                                                            contentDescription = myUsername,
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        if (!myFrame.isNullOrBlank() && myFrame != "none") {
                                                            val frameUrl = if (myFrame.startsWith("http")) myFrame else "https://cdn.jsdelivr.net/gh/SillyTavern/SillyTavern-AvatarDecorations@main/png/${myFrame}.png"
                                                            ApngDecorationImage(
                                                                url = frameUrl,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(34.dp)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Spacer(Modifier.width(34.dp))
                                                }
                                            }
                                        }

                                        // Actions & Reactions below the bubble
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = if (isMe) 40.dp else 40.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Like reaction pill
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(if (msg.isLikedByMe) Color(0xFFF43F5E).copy(alpha = 0.15f) else Color.Transparent)
                                                    .clickable { onLikePost(msg) }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (msg.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Like",
                                                    tint = if (msg.isLikedByMe) Color(0xFFF43F5E) else textMuted,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                if (msg.likes > 0) {
                                                    Text(
                                                        text = "${msg.likes}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (msg.isLikedByMe) Color(0xFFF43F5E) else textMuted
                                                    )
                                                }
                                            }

                                            // Edit & Delete for own messages
                                            if (isMe) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = textMuted,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            editingMessage = msg
                                                            editTextValue = msg.content
                                                        }
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = textMuted,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { deletePost(msg) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Scroll to Bottom Button
                val showScrollBottom by remember {
                    derivedStateOf { listState.canScrollForward }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollBottom,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(cardElevated)
                            .border(1.dp, borderColor, CircleShape)
                            .clickable {
                                scope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = activeChannel.tint,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Latest",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }
                }
            }

            // Bottom Composer Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardElevated)
                    .border(0.8.dp, borderColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Attached Tagged Show Preview
                if (taggedAnime != null) {
                    val show = taggedAnime!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(activeChannel.tint.copy(alpha = 0.12f))
                            .border(1.dp, activeChannel.tint.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AsyncImage(
                                model = show.cover,
                                contentDescription = show.title,
                                modifier = Modifier
                                    .size(width = 24.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(
                                    text = if (selectedChannelId == "Recommendations") "Recommending" else "Talking about",
                                    fontSize = 9.5.sp,
                                    color = textMuted
                                )
                                Text(
                                    text = show.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { taggedAnime = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove tag", tint = textMuted, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                // Helper Action Buttons: Tag Anime, Spoiler, Spin a Topic, Emojis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tag a show button
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (taggedAnime != null) activeChannel.tint.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp, if (taggedAnime != null) activeChannel.tint else borderColor, CircleShape)
                            .clickable { showTagSheet = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Movie,
                            contentDescription = null,
                            tint = if (taggedAnime != null) activeChannel.tint else textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (taggedAnime != null) "Change show" else "Tag a show",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (taggedAnime != null) activeChannel.tint else textMuted
                        )
                    }

                    // Spoiler wrapper button
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(1.dp, borderColor, CircleShape)
                            .clickable {
                                inputText = if (inputText.isNotBlank()) "||$inputText||" else "||spoiler||"
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Spoiler",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMuted
                        )
                    }

                    // Spin topic button
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(1.dp, borderColor, CircleShape)
                            .clickable {
                                val pool = activeChannel.starters.filter { it != inputText }
                                if (pool.isNotEmpty()) {
                                    inputText = pool.random()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Spin",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMuted
                        )
                    }

                    // Quick emojis row
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(EMOJIS) { em ->
                            Text(
                                text = em,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        if (inputText.length < MAX_MSG_LEN) {
                                            inputText += em
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Text Input and Send Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(pageBg)
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Message #${activeChannel.label}…",
                                fontSize = 13.sp,
                                color = textMuted
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = {
                                if (it.length <= MAX_MSG_LEN) inputText = it
                            },
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                color = textPrimary
                            ),
                            cursorBrush = SolidColor(activeChannel.tint),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                        )
                    }

                    if (inputText.length > MAX_MSG_LEN - 100) {
                        Text(
                            text = "${MAX_MSG_LEN - inputText.length}",
                            fontSize = 10.5.sp,
                            color = if (inputText.length >= MAX_MSG_LEN) Color.Red else textMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) activeChannel.tint else cardElevated)
                            .border(1.dp, if (inputText.isNotBlank()) activeChannel.tint else borderColor, CircleShape)
                            .clickable(enabled = inputText.isNotBlank() && !isSending) {
                                sendMessage()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) Color.White else textMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------- Room Pulse Sheet
    if (showPulseSheet) {
        val pulseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPulseSheet = false },
            sheetState = pulseSheetState,
            containerColor = if (isDark) Color(0xFF0F0F12) else Color.White
        ) {
            RoomPulseContent(
                messages = messages,
                channel = activeChannel,
                onAnimeClick = {
                    showPulseSheet = false
                    onAnimeClick?.invoke(it)
                },
                onClose = { showPulseSheet = false }
            )
        }
    }

    // ------------------------------------------------------------- Tag a Show Sheet
    if (showTagSheet) {
        val tagSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTagSheet = false },
            sheetState = tagSheetState,
            containerColor = if (isDark) Color(0xFF0F0F12) else Color.White
        ) {
            TagShowSheetContent(
                vm = vm,
                onSelectAnime = { selected ->
                    taggedAnime = selected
                    showTagSheet = false
                },
                onClose = { showTagSheet = false }
            )
        }
    }

    // ------------------------------------------------------------- Edit Message Dialog
    if (editingMessage != null) {
        val editTarget = editingMessage!!
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingMessage = null },
            sheetState = editSheetState,
            containerColor = if (isDark) Color(0xFF0F0F12) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Edit Message",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardElevated)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = editTextValue,
                        onValueChange = { if (it.length <= MAX_MSG_LEN) editTextValue = it },
                        textStyle = TextStyle(fontSize = 13.5.sp, color = textPrimary),
                        cursorBrush = SolidColor(textPrimary),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { editingMessage = null }) {
                        Text("Cancel", color = textMuted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeChannel.tint)
                            .clickable(enabled = editTextValue.isNotBlank() && !isSavingEdit) {
                                saveEdit(editTarget, editTextValue)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ Spoiler / RichText */

@Composable
private fun CommunityMessageRichText(
    content: String,
    msgId: String,
    isMe: Boolean,
    revealedSpoilers: MutableMap<String, Boolean>,
    textColor: Color
) {
    // Regex matches ||spoiler content||
    val parts = remember(content) {
        val list = mutableListOf<Pair<String, Boolean>>()
        val regex = Regex("\\|\\|(.*?)\\|\\|")
        var lastIdx = 0
        regex.findAll(content).forEach { match ->
            if (match.range.first > lastIdx) {
                list.add(Pair(content.substring(lastIdx, match.range.first), false))
            }
            list.add(Pair(match.groupValues[1], true))
            lastIdx = match.range.last + 1
        }
        if (lastIdx < content.length) {
            list.add(Pair(content.substring(lastIdx), false))
        }
        list
    }

    Column {
        parts.forEachIndexed { idx, pair ->
            val text = pair.first
            val isSpoiler = pair.second
            if (isSpoiler) {
                val spoilerKey = "$msgId:$idx"
                val isRevealed = revealedSpoilers[spoilerKey] == true
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRevealed) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.65f))
                        .border(0.8.dp, if (isRevealed) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .clickable {
                            revealedSpoilers[spoilerKey] = !isRevealed
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    if (isRevealed) {
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            color = textColor,
                            lineHeight = 18.sp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Spoiler (tap to reveal)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = textColor,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ Room Pulse Sheet Content */

@Composable
private fun RoomPulseContent(
    messages: List<CommunityMessage>,
    channel: CommunityChannel,
    onAnimeClick: (Int) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val textPrimary = colors.textPrimary
    val textSecondary = colors.textSecondary
    val textMuted = colors.textMuted
    val borderColor = colors.glass.border
    val cardElevated = colors.surfaceElevated

    // Calculate Pulse Stats
    val dayAgo = remember { System.currentTimeMillis() - 24 * 3600 * 1000 }
    val recentMsgs = remember(messages) {
        messages.filter {
            val d = parseIsoDate(it.createdAt)
            d != null && d.time > dayAgo
        }
    }
    val messagesTodayCount = recentMsgs.size
    val uniqueVoicesCount = remember(messages) {
        messages.map { it.username }.distinct().size
    }
    val mostLovedMsg = remember(messages) {
        messages.filter { it.likes > 0 }.maxByOrNull { it.likes }
    }
    val topShows = remember(messages) {
        val showCountMap = mutableMapOf<String, Triple<String, String, Int>>() // id -> (title, image, count)
        messages.forEach { m ->
            if (!m.animeId.isNullOrBlank() && !m.animeTitle.isNullOrBlank()) {
                val existing = showCountMap[m.animeId]
                val count = (existing?.third ?: 0) + 1
                showCountMap[m.animeId] = Triple(m.animeTitle, m.animeImage ?: "", count)
            }
        }
        showCountMap.toList().sortedByDescending { it.second.third }.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Room pulse",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "#${channel.label} live activity & highlights",
                    fontSize = 11.5.sp,
                    color = textMuted
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = textMuted)
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2 Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardElevated)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "$messagesTodayCount",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "messages today",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardElevated)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "$uniqueVoicesCount",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "voices in #${channel.label}",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Most Loved Card
        Text(
            text = "Most loved",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(Modifier.height(6.dp))
        if (mostLovedMsg != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "\"${mostLovedMsg.content}\"",
                        fontSize = 13.sp,
                        color = textSecondary,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "@${mostLovedMsg.username}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(13.dp))
                            Text(text = "${mostLovedMsg.likes}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Give a message a heart and it will be crowned here.",
                    fontSize = 12.sp,
                    color = textMuted
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Being Talked About Section
        Text(
            text = "Being talked about",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(Modifier.height(6.dp))
        if (topShows.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                topShows.forEach { (animeId, data) ->
                    val (title, image, count) = data
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardElevated)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable {
                                animeId.toIntOrNull()?.let { onAnimeClick(it) }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (image.isNotBlank()) {
                            AsyncImage(
                                model = image,
                                contentDescription = title,
                                modifier = Modifier
                                    .size(width = 30.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$count ${if (count == 1) "mention" else "mentions"}",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Tag a show in your message and it lands here for everyone to explore.",
                    fontSize = 12.sp,
                    color = textMuted
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Find Us Elsewhere
        Text(
            text = "Find us elsewhere",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Discord
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF5865F2).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF5865F2).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/kHxHzZtH7"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Discord", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5865F2))
            }

            // Telegram
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF229ED9).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF229ED9).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/StreamverseOfc"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Telegram", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF229ED9))
            }

            // GitHub
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardElevated)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nyyrox/zenkai"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
        }
    }
}

/* ------------------------------------------------------------------ Tag Show Sheet Content */

@Composable
private fun TagShowSheetContent(
    vm: MainViewModel,
    onSelectAnime: (Anime) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    val textPrimary = colors.textPrimary
    val textMuted = colors.textMuted
    val borderColor = colors.glass.border
    val cardElevated = colors.surfaceElevated

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isBlank()) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300)
        results = vm.searchAniListAnime(q)
        isSearching = false
    }

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
                text = "Tag a show",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = textMuted)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardElevated)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = textMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search AniList: Frieren, Dandadan…", fontSize = 13.sp, color = textMuted)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(fontSize = 13.sp, color = textPrimary),
                        cursorBrush = SolidColor(textPrimary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (isSearching) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = textPrimary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Results List
        if (results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(results, key = { it.id }) { anime ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardElevated)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable { onSelectAnime(anime) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = anime.cover,
                            contentDescription = anime.title,
                            modifier = Modifier
                                .size(width = 34.dp, height = 46.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = anime.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val meta = listOfNotNull(anime.format, anime.seasonYear?.toString(), anime.genres.firstOrNull()).joinToString(" · ")
                            if (meta.isNotBlank()) {
                                Text(
                                    text = meta,
                                    fontSize = 11.sp,
                                    color = textMuted
                                )
                            }
                        }
                    }
                }
            }
        } else if (query.isNotBlank() && !isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No shows found. Try another spelling.",
                    fontSize = 12.5.sp,
                    color = textMuted
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type an anime title and tap to attach it to your post.",
                    fontSize = 12.sp,
                    color = textMuted
                )
            }
        }
    }
}
