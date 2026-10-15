package com.nyyrox.aniverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

private val GLITCH_GLYPHS = charArrayOf('X', 'Z', '/', '<', '>', '_', '-', '1', '0', '#', '%', '!')

private fun glitchCharacters(original: String): String {
    val chars = original.toCharArray()
    val glitchCount = Random.nextInt(1, 3)
    repeat(glitchCount) {
        val idx = Random.nextInt(chars.size)
        if (chars[idx] != ' ') {
            chars[idx] = GLITCH_GLYPHS[Random.nextInt(GLITCH_GLYPHS.size)]
        }
    }
    return String(chars)
}

@Composable
fun ZenkaiGlitchText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    letterSpacing: TextUnit = 2.4.sp,
    fontFamily: FontFamily = ZenkaiFontFamily,
    baseColor: Color = Color(0xFFCAD0E0)
) {
    var isGlitching by remember { mutableStateOf(false) }
    var displayText by remember { mutableStateOf(text) }
    var offsetXRed by remember { mutableFloatStateOf(0f) }
    var offsetYRed by remember { mutableFloatStateOf(0f) }
    var offsetXCyan by remember { mutableFloatStateOf(0f) }
    var offsetYCyan by remember { mutableFloatStateOf(0f) }
    var glitchAlpha by remember { mutableFloatStateOf(0f) }
    var showScanline by remember { mutableStateOf(false) }
    var scanlineOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(text) {
        while (isActive) {
            // Idle stable state between glitch bursts: 1.8s - 2.8s
            isGlitching = false
            displayText = text
            glitchAlpha = 0f
            showScanline = false
            delay(Random.nextLong(1800, 2800))

            // Glitch burst: 4 to 6 quick frames (not too slow, not too fast)
            val burstFrames = Random.nextInt(4, 7)
            for (i in 0 until burstFrames) {
                isGlitching = true
                glitchAlpha = Random.nextFloat() * 0.35f + 0.55f
                offsetXRed = Random.nextInt(-4, 5).toFloat()
                offsetYRed = Random.nextInt(-1, 2).toFloat()
                offsetXCyan = (-offsetXRed * 0.85f) + Random.nextInt(-2, 3).toFloat()
                offsetYCyan = -offsetYRed
                showScanline = Random.nextFloat() < 0.55f
                scanlineOffset = Random.nextInt(-7, 8).toFloat()

                // Random character jitter in 40% of burst frames
                displayText = if (Random.nextFloat() < 0.40f) {
                    glitchCharacters(text)
                } else {
                    text
                }

                delay(Random.nextLong(55, 95))
            }

            // Quick secondary stutter in 50% of cycles
            if (Random.nextFloat() < 0.50f) {
                delay(70)
                isGlitching = true
                glitchAlpha = 0.5f
                offsetXRed = 3f
                offsetYRed = 0f
                offsetXCyan = -2.5f
                offsetYCyan = 0f
                displayText = text
                showScanline = false
                delay(50)
            }

            // Return to stable
            isGlitching = false
            displayText = text
            glitchAlpha = 0f
            showScanline = false
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Red Chromatic Aberration Layer
        if (isGlitching) {
            Text(
                text = displayText,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = letterSpacing,
                color = Color(0xFFFF1E27).copy(alpha = glitchAlpha * 0.80f),
                modifier = Modifier.offset(x = offsetXRed.dp, y = offsetYRed.dp)
            )

            // Cyan / Blue Chromatic Aberration Layer
            Text(
                text = displayText,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = letterSpacing,
                color = Color(0xFF00E5FF).copy(alpha = glitchAlpha * 0.70f),
                modifier = Modifier.offset(x = offsetXCyan.dp, y = offsetYCyan.dp)
            )
        }

        // Base Crisp Text Layer
        Text(
            text = displayText,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            color = baseColor
        )

        // Micro Scanline Slice when glitching
        if (isGlitching && showScanline) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(1.dp)
                    .offset(y = scanlineOffset.dp)
                    .background(Color(0x9000E5FF))
            )
        }
    }
}