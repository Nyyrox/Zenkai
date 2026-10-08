package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.ui.theme.LocalThemeColors
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-end anime liquid aura loading animation.
 * Features a breathing cursed energy core, dual orbital particle rings,
 * and shimmering energy motes.
 * Automatically adapts when Battery Saver or Reduced Motion is enabled.
 */
@Composable
fun AnimeLiquidLoader(
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    label: String? = "Extracting Streams..."
) {
    val themeColors = LocalThemeColors.current
    val themeSettings = LocalThemeSettings.current
    val isPowerSaving = themeSettings.isPowerSavingEnabled
    val isReducedMotion = themeSettings.isReducedMotionEnabled

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_aura")

    // Rotation angle for primary particle ring
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPowerSaving) 3200 else 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_cw"
    )

    // Counter-rotation angle for secondary ring
    val counterOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPowerSaving) 4500 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_ccw"
    )

    // Breathing pulse for the central energy orb
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val primaryColor = themeColors.primary
    val secondaryColor = themeColors.secondary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                val radius = (size.toPx() / 2f) - 6f

                // 1. Central Pulsing Liquid Core
                val coreRadius = (radius * 0.42f) * if (isReducedMotion) 1f else corePulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.85f),
                            secondaryColor.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = coreRadius * 1.5f
                    ),
                    radius = coreRadius * 1.5f,
                    center = center
                )

                drawCircle(
                    color = primaryColor,
                    radius = coreRadius * 0.65f,
                    center = center
                )

                // 2. Primary Orbital Ring (with dashed aura effect)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = radius * 0.82f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                if (!isPowerSaving) {
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.20f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1f)
                    )
                }

                // 3. Orbital Particles (Motes of Energy)
                val particleCount = if (isPowerSaving) 3 else 6
                val step = (2 * Math.PI) / particleCount
                for (i in 0 until particleCount) {
                    val angleRad = Math.toRadians((orbitAngle + (i * (360f / particleCount))).toDouble())
                    val orbitR = radius * 0.82f
                    val px = center.x + (orbitR * cos(angleRad)).toFloat()
                    val py = center.y + (orbitR * sin(angleRad)).toFloat()
                    val pAlpha = ((i + 1f) / particleCount).coerceIn(0.35f, 1f)

                    drawCircle(
                        color = primaryColor.copy(alpha = pAlpha),
                        radius = if (i == 0) 3.5f else 2.2f,
                        center = Offset(px, py)
                    )
                }

                // 4. Secondary Counter-Revolving Particles
                if (!isPowerSaving) {
                    for (j in 0 until 3) {
                        val counterRad = Math.toRadians((counterOrbitAngle + (j * 120f)).toDouble())
                        val px2 = center.x + (radius * cos(counterRad)).toFloat()
                        val py2 = center.y + (radius * sin(counterRad)).toFloat()

                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.8f),
                            radius = 2.5f,
                            center = Offset(px2, py2)
                        )
                    }
                }
            }
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = themeColors.primary
            )
        }
    }
}
