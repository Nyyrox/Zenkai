package com.nyyrox.aniverse.ui.components

import android.content.Intent
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.StorageManager
import com.nyyrox.aniverse.data.discord.DiscordAccountManager
import com.nyyrox.aniverse.data.discord.DiscordConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordIntegrationSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context) }
    val discordAccount by DiscordAccountManager.accountState.collectAsState()
    val rpcStatus by com.nyyrox.aniverse.data.discord.DiscordRpcService.rpcStatus.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var customToken by remember(discordAccount?.userToken) { mutableStateOf(discordAccount?.userToken.orEmpty()) }
    var isValidatingToken by remember { mutableStateOf(false) }
    var showTokenGuide by remember { mutableStateOf(false) }

    var clientIdInput by remember { mutableStateOf(storageManager.getDiscordClientId()) }
    var clientSecretInput by remember { mutableStateOf(storageManager.getDiscordClientSecret()) }
    var showCredentialsInput by remember { mutableStateOf(false) }
    var showDiscordLoginModal by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OttPanelHigh,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5865F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = OttTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Discord Rich Presence",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary
                        )
                        Text(
                            text = "Show what you're watching live on Discord",
                            fontSize = 12.sp,
                            color = OttInk(0.6f)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OttTextPrimary)
                }
            }

            // User Profile Card
            if (discordAccount != null) {
                val acc = discordAccount!!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OttPanelHigh)
                        .border(1.dp, Color(0x355865F2), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!acc.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = acc.avatarUrl,
                            contentDescription = acc.username,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF5865F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = acc.username.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = OttTextPrimary,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = acc.globalName ?: acc.username,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary
                        )
                        Text(
                            text = "@${acc.username}",
                            fontSize = 12.sp,
                            color = OttInk(0.65f)
                        )
                    }

                    Button(
                        onClick = { DiscordAccountManager.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF4D4D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Log Out", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Not logged in: OAuth Login Button
                Button(
                    onClick = {
                        showDiscordLoginModal = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = OttTextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Login with Discord", fontWeight = FontWeight.Bold, color = OttTextPrimary)
                }
            }

            // Rich Presence Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OttPanelHigh)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Rich Presence (RPC)",
                        color = OttTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Broadcasting anime title, episode & live timer",
                        color = OttInk(0.55f),
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = discordAccount?.isRpcEnabled ?: true,
                    onCheckedChange = { enabled ->
                        DiscordAccountManager.setRpcEnabled(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OttTextPrimary,
                        checkedTrackColor = Color(0xFF5865F2),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = OttInk(0.20f)
                    )
                )
            }

            // Live Preview Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OttPanelHigh)
                    .border(1.dp, OttInk(0.10f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCORD ACTIVITY PREVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5865F2),
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (rpcStatus.contains("Broadcasting")) Color(0xFF43B581) else Color(0xFFFAA81A))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = rpcStatus,
                            fontSize = 10.sp,
                            color = if (rpcStatus.contains("Broadcasting")) Color(0xFF43B581) else Color(0xFFFAA81A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF5865F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OttTextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Watching Zenkai",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary
                        )
                        Text(
                            text = "Solo Leveling Season 2",
                            fontSize = 11.sp,
                            color = OttInk(0.8f)
                        )
                        Text(
                            text = "Episode 4 (Sub) · 12:45 elapsed",
                            fontSize = 10.sp,
                            color = OttInk(0.5f)
                        )
                    }
                }
            }

            // Personal Discord Profile Status (User Token)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OttPanelHigh)
                    .border(1.dp, Color(0x205865F2), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Broadcast to Your Profile",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary
                        )
                        Text(
                            text = if (!discordAccount?.userToken.isNullOrBlank()) "Active: Personal user token linked" else "Currently broadcasting via Zenkai Bot",
                            fontSize = 11.sp,
                            color = if (!discordAccount?.userToken.isNullOrBlank()) Color(0xFF43B581) else OttInk(0.55f)
                        )
                    }
                    Text(
                        text = if (showTokenGuide) "Hide Guide" else "How to get?",
                        fontSize = 11.sp,
                        color = Color(0xFF5865F2),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showTokenGuide = !showTokenGuide }
                    )
                }

                if (showTokenGuide) {
                    Text(
                        text = "1. Log into discord.com in a web browser.\n2. Press F12 (Inspect) > Network tab.\n3. Search 'api' and refresh page.\n4. Click any request, copy 'Authorization' value under Request Headers.",
                        fontSize = 10.5.sp,
                        color = OttInk(0.7f),
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(OttPanelHigh)
                            .padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = customToken,
                    onValueChange = { customToken = it.trim() },
                    label = { Text("Discord User Token (optional)", fontSize = 11.sp) },
                    placeholder = { Text("Paste your Discord token here...", fontSize = 11.sp, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OttTextPrimary,
                        unfocusedTextColor = OttTextPrimary,
                        focusedBorderColor = Color(0xFF5865F2),
                        unfocusedBorderColor = OttInk(0.20f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (customToken.isBlank()) {
                                DiscordAccountManager.setUserToken(null)
                                Toast.makeText(context, "User token removed. Broadcasting via Zenkai Bot.", Toast.LENGTH_SHORT).show()
                            } else {
                                isValidatingToken = true
                                coroutineScope.launch {
                                    val res = DiscordAccountManager.validateDiscordToken(customToken)
                                    isValidatingToken = false
                                    if (res.isSuccess) {
                                        DiscordAccountManager.setUserToken(customToken)
                                        Toast.makeText(context, "Connected as @${res.getOrNull()}!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Token invalid! Please check and try again.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isValidatingToken,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isValidatingToken) "Validating..." else if (customToken.isNotBlank()) "Verify & Save Token" else "Use Zenkai Bot",
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary,
                            fontSize = 11.5.sp
                        )
                    }

                    if (!discordAccount?.userToken.isNullOrBlank()) {
                        Button(
                            onClick = {
                                customToken = ""
                                DiscordAccountManager.setUserToken(null)
                                Toast.makeText(context, "Switched back to Zenkai Bot", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF4D4D)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear", color = Color(0xFFFF5252), fontSize = 11.5.sp)
                        }
                    }
                }
            }

            // Advanced / Credentials Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCredentialsInput = !showCredentialsInput }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = OttInk(0.7f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Discord App Credentials",
                        fontSize = 12.sp,
                        color = OttInk(0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = if (showCredentialsInput) "Hide" else "Edit",
                    fontSize = 11.sp,
                    color = Color(0xFF5865F2),
                    fontWeight = FontWeight.Bold
                )
            }

            if (showCredentialsInput) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OttPanelHigh)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = clientIdInput,
                        onValueChange = { clientIdInput = it.trim() },
                        label = { Text("Application Client ID", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary,
                            focusedBorderColor = Color(0xFF5865F2),
                            unfocusedBorderColor = OttInk(0.20f)
                        )
                    )

                    OutlinedTextField(
                        value = clientSecretInput,
                        onValueChange = { clientSecretInput = it.trim() },
                        label = { Text("Client Secret / Key", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OttTextPrimary,
                            unfocusedTextColor = OttTextPrimary,
                            focusedBorderColor = Color(0xFF5865F2),
                            unfocusedBorderColor = OttInk(0.20f)
                        )
                    )

                    Button(
                        onClick = {
                            storageManager.saveDiscordClientId(clientIdInput)
                            storageManager.saveDiscordClientSecret(clientSecretInput)
                            Toast.makeText(context, "Discord credentials saved!", Toast.LENGTH_SHORT).show()
                            showCredentialsInput = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save App Credentials", fontWeight = FontWeight.Bold, color = OttTextPrimary)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showDiscordLoginModal) {
        DiscordLoginModal(
            onDismiss = { showDiscordLoginModal = false },
            onSuccess = { showDiscordLoginModal = false } // accountState StateFlow auto-refreshes card
        )
    }
}
