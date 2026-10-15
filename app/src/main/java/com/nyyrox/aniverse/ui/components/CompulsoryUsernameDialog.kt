package com.nyyrox.aniverse.ui.components

import androidx.activity.compose.BackHandler
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttOnPrimary
import com.nyyrox.aniverse.ui.theme.OttPrimary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun CompulsoryUsernameDialog(
    vm: MainViewModel,
    onUsernameSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        // Compulsory: do not dismiss
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE6060709))
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        CompulsoryUsernameCard(
            vm = vm,
            onUsernameSet = onUsernameSet,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
fun CompulsoryUsernameCard(
    vm: MainViewModel,
    onUsernameSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var usernameInput by remember {
        val initial = vm.userProfile.username.ifBlank {
            val fromDisplay = vm.userProfile.displayName
                .filter { it.isLetterOrDigit() || it == '_' }
                .lowercase()
            val fromEmail = vm.userProfile.email?.substringBefore("@")
                ?.filter { it.isLetterOrDigit() || it == '_' }
                ?.lowercase()
            (fromDisplay.takeIf { it.isNotBlank() && it != "guest" && it != "otaku" } ?: fromEmail ?: "")
                .take(16)
        }
        mutableStateOf(initial)
    }

    var isCheckingAvailability by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf<Boolean?>(null) }
    var availabilityMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cleanUsername = remember(usernameInput) {
        usernameInput.trim().lowercase().removePrefix("@")
    }

    val isValidFormat = remember(cleanUsername) {
        cleanUsername.length in 3..20 && Regex("^[a-z0-9_]+$").matches(cleanUsername)
    }

    LaunchedEffect(cleanUsername) {
        errorMessage = null
        if (cleanUsername.isBlank()) {
            isAvailable = null
            availabilityMessage = null
            isCheckingAvailability = false
            return@LaunchedEffect
        }

        if (cleanUsername.length < 3) {
            isAvailable = false
            availabilityMessage = "Minimum 3 characters required"
            isCheckingAvailability = false
            return@LaunchedEffect
        }

        if (cleanUsername.length > 20) {
            isAvailable = false
            availabilityMessage = "Maximum 20 characters allowed"
            isCheckingAvailability = false
            return@LaunchedEffect
        }

        if (!Regex("^[a-z0-9_]+$").matches(cleanUsername)) {
            isAvailable = false
            availabilityMessage = "Only letters, numbers, and underscores allowed"
            isCheckingAvailability = false
            return@LaunchedEffect
        }

        isCheckingAvailability = true
        availabilityMessage = "Checking availability..."
        delay(350)
        try {
            val available = vm.checkUsernameAvailability(cleanUsername)
            isAvailable = available
            availabilityMessage = if (available) {
                "✓ @$cleanUsername is available!"
            } else {
                "✗ @$cleanUsername is already taken"
            }
        } catch (e: Exception) {
            isAvailable = true
            availabilityMessage = null
        } finally {
            isCheckingAvailability = false
        }
    }

    val suggestions = remember(vm.userProfile.displayName, vm.userProfile.email) {
        val base = (vm.userProfile.displayName.takeIf { it.isNotBlank() && it != "Guest" && it != "Otaku" }
            ?: vm.userProfile.email?.substringBefore("@") ?: "otaku")
            .filter { it.isLetterOrDigit() || it == '_' }
            .lowercase()
            .take(12)
        listOf("${base}_01", "${base}_x", "${base}_77", "real_${base}").distinct()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OttPanelHigh)
            .border(BorderStroke(1.dp, OttInk(0.19f)), RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(id = R.drawable.zenkai_navbar_logo),
                contentDescription = "ZENKAI",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(32.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1CE11D48))
                    .border(1.dp, Color(0x50E11D48), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "COMPULSORY ACCOUNT SETUP",
                        color = Color(0xFFF43F5E),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Enter Your Username",
                color = OttTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Every ZENKAI user is required to choose a unique username to proceed. This handle identifies you across community features and cloud sync.",
                color = Color(0xFF94A3B8),
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22EF4444))
                            .border(BorderStroke(1.dp, Color(0x66EF4444)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { input ->
                    val filtered = input.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '@' }
                    if (filtered.length <= 21) {
                        usernameInput = filtered.removePrefix("@")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                placeholder = { Text("your_username") },
                leadingIcon = {
                    Icon(
                        Icons.Default.AlternateEmail,
                        contentDescription = null,
                        tint = if (isAvailable == true) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (isCheckingAvailability) {
                        CircularProgressIndicator(
                            color = OttTextPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (cleanUsername.isNotBlank()) {
                        if (isAvailable == true && isValidFormat) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Available",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (isAvailable == false || !isValidFormat) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Unavailable",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isAvailable == true) Color(0xFF4ADE80) else OttTextPrimary,
                    unfocusedBorderColor = if (isAvailable == true) Color(0x664ADE80) else OttInk(0.13f),
                    focusedContainerColor = OttInk(0.06f),
                    unfocusedContainerColor = OttInk(0.05f),
                    focusedTextColor = OttTextPrimary,
                    unfocusedTextColor = OttTextPrimary,
                    cursorColor = OttTextPrimary,
                    focusedLabelColor = if (isAvailable == true) Color(0xFF4ADE80) else OttTextPrimary,
                    unfocusedLabelColor = Color(0xFF64748B)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            if (availabilityMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = availabilityMessage ?: "",
                    color = when {
                        isCheckingAvailability -> Color(0xFF94A3B8)
                        isAvailable == true -> Color(0xFF4ADE80)
                        else -> Color(0xFFEF4444)
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Suggestions:",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.take(3).forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(OttInk(0.08f))
                                .border(1.dp, OttInk(0.14f), RoundedCornerShape(8.dp))
                                .clickable { usernameInput = suggestion }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "@$suggestion",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val canSubmit = isValidFormat && isAvailable == true && !isSubmitting && !isCheckingAvailability
            Button(
                onClick = {
                    if (canSubmit) {
                        isSubmitting = true
                        errorMessage = null
                        vm.submitUsername(cleanUsername) { success, err ->
                            isSubmitting = false
                            if (success) {
                                onUsernameSet()
                            } else {
                                errorMessage = err ?: "Failed to set username"
                            }
                        }
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OttPrimary,
                    contentColor = OttOnPrimary,
                    disabledContainerColor = OttInk(0.12f),
                    disabledContentColor = OttInk(0.25f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Confirm Username & Continue",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
