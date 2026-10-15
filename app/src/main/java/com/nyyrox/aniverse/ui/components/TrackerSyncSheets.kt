package com.nyyrox.aniverse.ui.components

import android.annotation.SuppressLint
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import android.graphics.Bitmap
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.tracker.AniListAccount
import com.nyyrox.aniverse.data.tracker.AniListTrackerClient
import com.nyyrox.aniverse.data.tracker.MalAccount
import com.nyyrox.aniverse.data.tracker.MalTrackerClient
import com.nyyrox.aniverse.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniListSyncModalSheet(
    vm: MainViewModel,
    account: AniListAccount?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OttPanelHigh,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF02A9FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = OttTextPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "AniList Sync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OttTextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OttTextPrimary)
                }
            }

            if (account != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OttPanelHigh)
                        .border(1.dp, Color(0x2502A9FF), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!account.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = account.avatarUrl,
                            contentDescription = account.username,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.username,
                            color = OttTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (account.accessToken.isNotBlank()) "Linked (Full Two-Way Sync)" else "Linked (Public Sync)",
                            color = Color(0xFF02A9FF),
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        vm.syncTrackers {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF02A9FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !vm.isTrackerSyncing
                ) {
                    if (vm.isTrackerSyncing) {
                        CircularProgressIndicator(color = OttTextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Syncing Watchlist...", color = OttTextPrimary, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = OttTextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Full AniList Watchlist", color = OttTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        vm.disconnectAniList()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    border = BorderStroke(1.dp, Color(0x33FF4D4D))
                ) {
                    Text("Disconnect AniList", color = Color(0xFFFF4D4D), fontWeight = FontWeight.SemiBold)
                }
            } else {
                var selectedTab by remember { mutableIntStateOf(0) }
                var tokenInput by remember { mutableStateOf("") }
                var usernameInput by remember { mutableStateOf("") }
                var statusMsg by remember { mutableStateOf<String?>(null) }
                var isConnecting by remember { mutableStateOf(false) }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = OttPanelHigh,
                    contentColor = OttTextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF02A9FF)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Access Token", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Username", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("In-App Login", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        Text(
                            text = "Paste your AniList Personal Access Token for full two-way sync (reading & updating watch progress, scores, and status).",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            placeholder = { Text("Paste AniList Token here", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF02A9FF),
                                unfocusedBorderColor = OttInk(0.13f),
                                focusedTextColor = OttTextPrimary,
                                unfocusedTextColor = OttTextPrimary
                            )
                        )

                        if (!statusMsg.isNullOrBlank()) {
                            Text(text = statusMsg.orEmpty(), color = Color(0xFF02A9FF), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (tokenInput.isNotBlank()) {
                                    isConnecting = true
                                    statusMsg = "Verifying token with AniList..."
                                    vm.handleAniListAuthToken(tokenInput) { success, msg ->
                                        isConnecting = false
                                        statusMsg = msg
                                        if (success) {
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            enabled = tokenInput.isNotBlank() && !isConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF02A9FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(color = OttTextPrimary, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Connect & Sync AniList", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    1 -> {
                        Text(
                            text = "Enter your public AniList username to instantly import all your anime lists into Zenkai without needing a password or token.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            placeholder = { Text("e.g. your_anilist_name", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF02A9FF),
                                unfocusedBorderColor = OttInk(0.13f),
                                focusedTextColor = OttTextPrimary,
                                unfocusedTextColor = OttTextPrimary
                            )
                        )

                        if (!statusMsg.isNullOrBlank()) {
                            Text(text = statusMsg.orEmpty(), color = Color(0xFF02A9FF), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (usernameInput.isNotBlank()) {
                                    isConnecting = true
                                    statusMsg = "Fetching AniList profile & watchlist..."
                                    vm.connectAniListByUsername(usernameInput) { success, msg ->
                                        isConnecting = false
                                        statusMsg = msg
                                        if (success) {
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            enabled = usernameInput.isNotBlank() && !isConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF02A9FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(color = OttTextPrimary, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Import AniList Watchlist", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(460.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OttTextPrimary)
                        ) {
                            InAppTrackerWebView(
                                url = AniListTrackerClient.AUTH_URL,
                                onTokenIntercepted = { token ->
                                    vm.handleAniListAuthToken(token) { success, _ ->
                                        if (success) onDismiss()
                                    }
                                },
                                onCodeIntercepted = null
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MalSyncModalSheet(
    vm: MainViewModel,
    account: MalAccount?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OttPanelHigh,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E51A2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, tint = OttTextPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "MyAnimeList (MAL) Sync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OttTextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OttTextPrimary)
                }
            }

            if (account != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OttPanelHigh)
                        .border(1.dp, Color(0x252E51A2), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!account.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = account.avatarUrl,
                            contentDescription = account.username,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.username,
                            color = OttTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (account.accessToken.isNotBlank()) "Linked (Full Sync)" else "Linked (Public Sync)",
                            color = Color(0xFF4C75D8),
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        vm.syncTrackers {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E51A2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !vm.isTrackerSyncing
                ) {
                    if (vm.isTrackerSyncing) {
                        CircularProgressIndicator(color = OttTextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Syncing Watchlist...", color = OttTextPrimary, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, tint = OttTextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Full MAL Watchlist", color = OttTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        vm.disconnectMal()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    border = BorderStroke(1.dp, Color(0x33FF4D4D))
                ) {
                    Text("Disconnect MyAnimeList", color = Color(0xFFFF4D4D), fontWeight = FontWeight.SemiBold)
                }
            } else {
                var selectedTab by remember { mutableIntStateOf(0) }
                var usernameInput by remember { mutableStateOf("") }
                var tokenInput by remember { mutableStateOf("") }
                var statusMsg by remember { mutableStateOf<String?>(null) }
                var isConnecting by remember { mutableStateOf(false) }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = OttPanelHigh,
                    contentColor = OttTextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF2E51A2)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Username", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("In-App Login", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Token", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        Text(
                            text = "Enter your MyAnimeList username to instantly import your full anime library, ratings, and progress into Zenkai.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            placeholder = { Text("e.g. your_mal_username", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2E51A2),
                                unfocusedBorderColor = OttInk(0.13f),
                                focusedTextColor = OttTextPrimary,
                                unfocusedTextColor = OttTextPrimary
                            )
                        )

                        if (!statusMsg.isNullOrBlank()) {
                            Text(text = statusMsg.orEmpty(), color = Color(0xFF4C75D8), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (usernameInput.isNotBlank()) {
                                    isConnecting = true
                                    statusMsg = "Fetching MAL user watchlist..."
                                    vm.connectMalByUsername(usernameInput) { success, msg ->
                                        isConnecting = false
                                        statusMsg = msg
                                        if (success) {
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            enabled = usernameInput.isNotBlank() && !isConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E51A2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(color = OttTextPrimary, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Import MyAnimeList Watchlist", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(460.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OttTextPrimary)
                        ) {
                            InAppTrackerWebView(
                                url = MalTrackerClient.getOAuthUrl(),
                                onTokenIntercepted = null,
                                onCodeIntercepted = { code ->
                                    vm.handleMalAuthCode(code) { success, _ ->
                                        if (success) onDismiss()
                                    }
                                }
                            )
                        }
                    }
                    2 -> {
                        Text(
                            text = "Paste a MAL OAuth Access Token directly for full two-way synchronization.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            placeholder = { Text("Paste MAL Token here", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2E51A2),
                                unfocusedBorderColor = OttInk(0.13f),
                                focusedTextColor = OttTextPrimary,
                                unfocusedTextColor = OttTextPrimary
                            )
                        )

                        if (!statusMsg.isNullOrBlank()) {
                            Text(text = statusMsg.orEmpty(), color = Color(0xFF4C75D8), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (tokenInput.isNotBlank()) {
                                    isConnecting = true
                                    statusMsg = "Connecting MAL account..."
                                    vm.handleMalAuthToken(tokenInput) { success, msg ->
                                        isConnecting = false
                                        statusMsg = msg
                                        if (success) {
                                            onDismiss()
                                        }
                                    }
                                }
                            },
                            enabled = tokenInput.isNotBlank() && !isConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E51A2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(color = OttTextPrimary, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Connect Token", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun InAppTrackerWebView(
    url: String,
    onTokenIntercepted: ((String) -> Unit)?,
    onCodeIntercepted: ((String) -> Unit)?
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                isNestedScrollingEnabled = true
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                // Prevent the enclosing ModalBottomSheet gesture handler from intercepting vertical scroll gestures
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val reqUrl = request?.url?.toString().orEmpty()
                        return handleIntercept(reqUrl)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { handleIntercept(it) }
                    }

                    private fun handleIntercept(checkUrl: String): Boolean {
                        if (checkUrl.contains("access_token=") && onTokenIntercepted != null) {
                            val token = checkUrl.substringAfter("access_token=").substringBefore("&").substringBefore("#")
                            if (token.isNotBlank()) {
                                onTokenIntercepted.invoke(token)
                                return true
                            }
                        }
                        if (checkUrl.contains("code=") && onCodeIntercepted != null) {
                            val code = checkUrl.substringAfter("code=").substringBefore("&").substringBefore("#")
                            if (code.isNotBlank()) {
                                onCodeIntercepted.invoke(code)
                                return true
                            }
                        }
                        return false
                    }
                }
                loadUrl(url)
            }
        }
    )
}
