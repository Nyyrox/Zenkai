package com.nyyrox.aniverse.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nyyrox.aniverse.data.discord.DiscordAccountManager
import com.nyyrox.aniverse.data.discord.DiscordConfig
import com.nyyrox.aniverse.ui.screens.DiscordLogo
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscordLoginModal(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isExchanging by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    // Shown when Discord's 2FA/MFA screen is detected — guides user to TOTP instead of passkeys
    var showMfaBanner by remember { mutableStateOf(false) }

    val authUrl = DiscordConfig.getAuthorizeUrl()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 14.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F121B))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161A26))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DiscordLogo(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Discord Login",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }




                if (isExchanging) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF5865F2))
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Authorizing Discord Account...",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (errorText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Login Failed",
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorText ?: "An error occurred during authentication",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.5.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2))
                            ) {
                                Text("Close", color = Color.White)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.userAgentString = settings.userAgentString.replace("; wv", "")

                                    webViewClient = object : WebViewClient() {
                                        private var codeHandled = false

                                        /** True when the URL signals Discord's 2FA/MFA flow */
                                        private fun isMfaUrl(url: String?) =
                                            url != null && (
                                                url.contains("/mfa") ||
                                                url.contains("two-factor") ||
                                                url.contains("2fa", ignoreCase = true)
                                            )

                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            if (isMfaUrl(url)) showMfaBanner = true
                                            if (checkAndHandleCode(url)) return
                                        }

                                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                            val headers = request?.requestHeaders
                                            val auth = headers?.get("Authorization") ?: headers?.get("authorization")
                                            if (!auth.isNullOrBlank() && !auth.startsWith("Bearer ") && !auth.startsWith("Bot ") && auth.length > 20) {
                                                DiscordAccountManager.setUserToken(auth)
                                            }
                                            return super.shouldInterceptRequest(view, request)
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString()
                                            if (isMfaUrl(url)) showMfaBanner = true
                                            return checkAndHandleCode(url)
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                            if (isMfaUrl(url)) showMfaBanner = true
                                            // JS sniff: detect MFA screen when URL doesn't change
                                            view?.evaluateJavascript(
                                                "(function(){ var t=document.body?document.body.innerText:''; return (t.indexOf('Multi-Factor')>=0||t.indexOf('passkey')>=0)?'mfa':'ok'; })();"
                                            ) { r -> if (r?.contains("mfa") == true) showMfaBanner = true }
                                            view?.evaluateJavascript(
                                                "(function(){ try { var t = window.localStorage.getItem('token'); if (t) return t.replace(/^\"|\"$/g, ''); } catch(e){} return ''; })();"
                                            ) { tokenJson ->
                                                val t = tokenJson?.trim('"', ' ', '\\')
                                                if (!t.isNullOrBlank() && t != "null" && t.length > 20) {
                                                    DiscordAccountManager.setUserToken(t)
                                                }
                                            }
                                        }

                                        private fun checkAndHandleCode(url: String?): Boolean {
                                            if (url == null || codeHandled) return false
                                            // Auto-dismiss banner when user leaves MFA page
                                            if (showMfaBanner && !isMfaUrl(url)) showMfaBanner = false
                                            if (url.startsWith(DiscordConfig.REDIRECT_URI) || 
                                                url.startsWith("zenkai://") || 
                                                url.startsWith("aniverse://") || 
                                                url.contains("/auth/v1/callback") ||
                                                url.contains("code=") ||
                                                url.contains("provider_token=") ||
                                                url.contains("access_token=")) {
                                                val uri = Uri.parse(url)
                                                val fragment = uri.fragment.orEmpty()
                                                val fragmentParams = fragment.split("&").associate { param ->
                                                    val parts = param.split("=")
                                                    parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" }
                                                }
                                                val providerToken = uri.getQueryParameter("provider_token")
                                                    ?: fragmentParams["provider_token"]
                                                    ?: uri.getQueryParameter("access_token")
                                                    ?: fragmentParams["access_token"]

                                                val code = uri.getQueryParameter("code")
                                                    ?: fragmentParams["code"]

                                                if (!providerToken.isNullOrBlank()) {
                                                    codeHandled = true
                                                    isExchanging = true
                                                    coroutineScope.launch {
                                                        val res = DiscordAccountManager.fetchProfileWithToken(providerToken)
                                                        if (res.isSuccess) {
                                                            onSuccess()
                                                            onDismiss()
                                                        } else {
                                                            errorText = res.exceptionOrNull()?.message ?: "Failed to fetch Discord profile"
                                                            isExchanging = false
                                                        }
                                                    }
                                                    return true
                                                } else if (!code.isNullOrBlank()) {
                                                    codeHandled = true
                                                    isExchanging = true
                                                    coroutineScope.launch {
                                                        val res = DiscordAccountManager.exchangeCodeForToken(code)
                                                        if (res.isSuccess) {
                                                            onSuccess()
                                                            onDismiss()
                                                        } else {
                                                            errorText = res.exceptionOrNull()?.message ?: "Authorization failed"
                                                            isExchanging = false
                                                        }
                                                    }
                                                    return true
                                                }
                                            }
                                            return false
                                        }
                                    }

                                    loadUrl(authUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0F121B)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF5865F2))
                            }
                        }
                    }
                }
            }
        }
    }
}
