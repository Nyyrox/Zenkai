package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.viewmodel.MainViewModel
import com.nyyrox.aniverse.ui.components.AniListSyncModalSheet
import com.nyyrox.aniverse.ui.components.MalSyncModalSheet
import com.nyyrox.aniverse.ui.components.CompulsoryUsernameCard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import com.nyyrox.aniverse.ui.components.DiscordLoginModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    vm: MainViewModel,
    onLoginSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var showAniListSheet by remember { mutableStateOf(false) }
    var showMalSheet by remember { mutableStateOf(false) }
    var showUsernameSetup by remember { mutableStateOf(false) }
    var showDiscordLoginModal by remember { mutableStateOf(false) }
    val authState = vm.authState

    val context = LocalContext.current
    val googleSignInClient = remember(context) {
        val serverClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "762562909877-ensgo6bk5nhjn8l8alp8sdn6b2h56k6p.apps.googleusercontent.com"
        } catch (_: Exception) {
            "762562909877-ensgo6bk5nhjn8l8alp8sdn6b2h56k6p.apps.googleusercontent.com"
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val idToken = account.idToken
                val acctEmail = account.email ?: ""
                val acctName = account.displayName ?: acctEmail.substringBefore("@")
                val photoUrl = account.photoUrl?.toString() ?: ""
                vm.handleGoogleSignInSuccess(
                    userId = account.id ?: "",
                    email = acctEmail,
                    displayName = acctName,
                    photoUrl = photoUrl,
                    idToken = idToken
                )
            } else {
                isGoogleSigningIn = false
            }
        } catch (e: ApiException) {
            isGoogleSigningIn = false
            if (e.statusCode != 12501 && e.statusCode != 12502) {
                vm.handleGoogleSignInError("Google sign in failed (code ${e.statusCode})")
            }
        } catch (e: Exception) {
            isGoogleSigningIn = false
            vm.handleGoogleSignInError(e.message ?: "Google sign in failed")
        }
    }

    // Handle success transition
    LaunchedEffect(authState, vm.authLoading, vm.isUsernameSetupRequired, vm.userProfile.username) {
        if (authState is AuthState.LoggedIn && !vm.authLoading) {
            isGoogleSigningIn = false
            if (vm.isUsernameSetupRequired || vm.userProfile.username.isBlank()) {
                showUsernameSetup = true
            } else {
                onLoginSuccess()
            }
        } else if (authState is AuthState.Error) {
            isGoogleSigningIn = false
            showUsernameSetup = false
        }
    }

    BackHandler(enabled = showUsernameSetup) {
        // Compulsory: do not allow back out until username is chosen
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060709))
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        if (showUsernameSetup) {
            CompulsoryUsernameCard(
                vm = vm,
                onUsernameSet = {
                    showUsernameSetup = false
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Glassmorphic Main Card ──────────────────────────────────────
                Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x0CFFFFFF))
                    .border(BorderStroke(1.dp, Color(0x18FFFFFF)), RoundedCornerShape(24.dp))
                    .padding(horizontal = 22.dp, vertical = 28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ZENKAI Brand Logo (from navbar PNG)
                    Image(
                        painter = painterResource(id = R.drawable.zenkai_navbar_logo),
                        contentDescription = "ZENKAI",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isSignUp) "Create your cloud account" else "Sign in to sync your watchlist & history",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = authState is AuthState.Error,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        if (authState is AuthState.Error) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x18FF453A))
                                    .border(1.dp, Color(0x40FF453A), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B6B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = authState.message,
                                        color = Color(0xFFFF9999),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Input Field Styling (Monochrome Glassmorphic)
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x22FFFFFF),
                        focusedContainerColor = Color(0x10FFFFFF),
                        unfocusedContainerColor = Color(0x0CFFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedPlaceholderColor = Color(0xFF64748B),
                        unfocusedPlaceholderColor = Color(0xFF64748B)
                    )

                    // Display Name (Only in Sign Up Mode)
                    AnimatedVisibility(
                        visible = isSignUp,
                        enter = fadeIn(tween(250)) + slideInVertically(tween(250)),
                        exit = fadeOut(tween(200)) + slideOutVertically(tween(200))
                    ) {
                        Column {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Display Name") },
                                placeholder = { Text("e.g. Mahfooz") },
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Address") },
                        placeholder = { Text("otaku@example.com") },
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Action Button: Crisp Solid White with Bold Black Text
                    Button(
                        onClick = {
                            if (isSignUp) {
                                vm.signUp(email, password, displayName)
                            } else {
                                vm.signIn(email, password)
                            }
                        },
                        enabled = authState !is AuthState.Loading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0x22FFFFFF),
                            disabledContentColor = Color(0xFF64748B)
                        )
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "Create Account" else "Sign In",
                                color = if (email.isNotBlank() && password.isNotBlank()) Color.Black else Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google OAuth Button: Frosted Glass Pill with Official Colors and Circular Animated Progress
                    val isGoogleLoading = isGoogleSigningIn || (authState is AuthState.Loading && vm.authLoading)
                    Button(
                        onClick = {
                            isGoogleSigningIn = true
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleAuthLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        enabled = !isGoogleLoading && authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGoogleLoading) Color(0x16FFFFFF) else Color(0x0EFFFFFF),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (isGoogleLoading) Color(0x404285F4) else Color(0x20FFFFFF))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isGoogleLoading) {
                                GoogleCircularLoader(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                GoogleLoadingText(text = "Connecting to Google...")
                            } else {
                                GoogleLogo(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Discord OAuth Button: Frosted Glass with Loading Animation
                    val isDiscordLoading = showDiscordLoginModal
                    Button(
                        onClick = { showDiscordLoginModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDiscordLoading) Color(0x16FFFFFF) else Color(0x0EFFFFFF),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (isDiscordLoading) Color(0x405865F2) else Color(0x20FFFFFF))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isDiscordLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF5865F2),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Opening Discord...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF5865F2)
                                )
                            } else {
                                DiscordLogo(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Discord",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Side-by-side AniList & MAL Sync Login Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAniListSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x1502A9FF),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0x4002A9FF))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color(0xFF02A9FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AniList Sync",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { showMalSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x152E51A2),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0x402E51A2))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.SyncAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF4C75D8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MAL Sync",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Toggle Sign In / Sign Up
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account?" else "Don't have an account?",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSignUp) "Sign In" else "Sign Up",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isSignUp = !isSignUp }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (showAniListSheet) {
                AniListSyncModalSheet(
                    vm = vm,
                    account = vm.aniListAccount,
                    onDismiss = {
                        showAniListSheet = false
                        if (vm.aniListAccount != null) {
                            onLoginSuccess()
                        }
                    }
                )
            }

            if (showMalSheet) {
                MalSyncModalSheet(
                    vm = vm,
                    account = vm.malAccount,
                    onDismiss = {
                        showMalSheet = false
                        if (vm.malAccount != null) {
                            onLoginSuccess()
                        }
                    }
                )
            }

            if (showDiscordLoginModal) {
                DiscordLoginModal(
                    onDismiss = { showDiscordLoginModal = false },
                    onSuccess = {
                        showDiscordLoginModal = false
                        val acc = com.nyyrox.aniverse.data.discord.DiscordAccountManager.accountState.value
                        if (acc != null) {
                            vm.handleDiscordSignIn(acc)
                        }
                        onLoginSuccess()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

                // Continue as Guest (Non-obtrusive Text Button)
                TextButton(
                    onClick = onContinueAsGuest,
                    shape = OttPillShape
                ) {
                    Text(
                        text = "Continue as Guest",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Renders the official 4-color Google "G" logo via vector drawable with pixel-perfect brand colors.
 */
@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_google_logo),
        contentDescription = "Google",
        modifier = modifier
    )
}

/**
 * Renders the official Discord logo via vector drawable with pixel-perfect brand styling.
 */
@Composable
fun DiscordLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_discord_logo),
        contentDescription = "Discord",
        modifier = modifier
    )
}

/**
 * High-quality 4-color Google rotating circular ring loader.
 */
@Composable
fun GoogleCircularLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "GoogleLoader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    Canvas(modifier = modifier.graphicsLayer { rotationZ = angle }) {
        val stroke = 2.8.dp.toPx()
        val sweepBrush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFF4285F4), // Blue
                Color(0xFFEA4335), // Red
                Color(0xFFFBBC05), // Yellow
                Color(0xFF34A853), // Green
                Color(0xFF4285F4)  // Blue
            )
        )
        drawCircle(
            brush = sweepBrush,
            radius = (size.minDimension / 2f) - (stroke / 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * High-quality animated text with multi-color shimmer wave.
 */
@Composable
fun GoogleLoadingText(
    modifier: Modifier = Modifier,
    text: String = "Connecting to Google..."
) {
    val transition = rememberInfiniteTransition(label = "TextShimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Shimmer"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF94A3B8),
            Color(0xFF4285F4),
            Color(0xFFEA4335),
            Color(0xFFFBBC05),
            Color(0xFF34A853),
            Color.White,
            Color(0xFF94A3B8)
        ),
        start = Offset(shimmerOffset * 400f, 0f),
        end = Offset(shimmerOffset * 400f + 250f, 60f)
    )

    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        style = TextStyle(brush = brush),
        modifier = modifier
    )
}

