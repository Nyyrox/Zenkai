package com.nyyrox.aniverse.ui.components.decorations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.cos
import kotlin.math.sin

data class BannerEffectPreset(
    val id: String,
    val name: String,
    val description: String
)

val BANNER_EFFECT_PRESETS = listOf(
    BannerEffectPreset("none", "None", "Clean banner with no overlays"),
    BannerEffectPreset("sakura", "Sakura Petals", "Soft cherry blossom petals gracefully drifting"),
    BannerEffectPreset("butterflies", "Butterflies", "Luminous fairy butterflies fluttering around"),
    BannerEffectPreset("starlight", "Starlight Fireflies", "Pulsing golden embers and starlight orbs"),
    BannerEffectPreset("embers", "Fiery Sparks", "Rising volcanic flame sparks and embers"),
    BannerEffectPreset("snow", "Celestial Snow", "Gentle crystalline winter snowfall")
)

@Composable
fun BannerOverlayRenderer(
    effectId: String?,
    modifier: Modifier = Modifier
) {
    if (effectId.isNullOrBlank() || effectId == "none") return

    val transition = rememberInfiniteTransition(label = "BannerOverlay")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BannerProgress"
    )

    Box(modifier = modifier) {
        when (effectId.lowercase()) {
            "sakura", "sakura_petals" -> SakuraPetalsCanvas(progress = progress)
            "butterflies", "butterfly" -> ButterfliesCanvas(progress = progress)
            "starlight", "fireflies" -> StarlightCanvas(progress = progress)
            "embers", "fire" -> EmbersCanvas(progress = progress)
            "snow", "snowfall" -> SnowCanvas(progress = progress)
        }
    }
}

private class Petal(
    val startXRatio: Float,
    val startYRatio: Float,
    val speedY: Float,
    val speedX: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val size: Float,
    val rotationSpeed: Float,
    val baseColor: Color
)

@Composable
private fun SakuraPetalsCanvas(progress: Float) {
    val petals = remember {
        val colors = listOf(
            Color(0xFFFFB7C5),
            Color(0xFFFF8DA1),
            Color(0xFFFFAEC9),
            Color(0xFFFFC0CB)
        )
        List(20) { i ->
            Petal(
                startXRatio = (i * 0.052f + (i * 0.17f) % 1f) % 1f,
                startYRatio = (i * 0.063f) % 1f,
                speedY = 0.45f + ((i * 13) % 10) * 0.04f,
                speedX = -0.15f - ((i * 7) % 10) * 0.02f,
                swayAmp = 20f + (i % 5) * 8f,
                swayFreq = 2f + (i % 4) * 0.8f,
                size = 12f + (i % 6) * 3f,
                rotationSpeed = 180f + (i % 7) * 45f,
                baseColor = colors[i % colors.size]
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        petals.forEachIndexed { i, p ->
            val t = (progress * p.speedY + p.startYRatio) % 1f
            val y = t * canvasHeight
            val sway = sin((progress * p.swayFreq + i) * Math.PI.toFloat() * 2f) * p.swayAmp
            val x = (p.startXRatio * canvasWidth + p.speedX * progress * canvasWidth + sway)
                .let { ((it % canvasWidth) + canvasWidth) % canvasWidth }

            val rot = (progress * p.rotationSpeed + i * 40f) % 360f

            rotate(degrees = rot, pivot = Offset(x, y)) {
                // Draw delicate sakura petal shape
                val path = Path().apply {
                    moveTo(x, y - p.size)
                    cubicTo(
                        x + p.size * 0.6f, y - p.size * 0.6f,
                        x + p.size * 0.6f, y + p.size * 0.4f,
                        x, y + p.size * 0.8f
                    )
                    cubicTo(
                        x - p.size * 0.6f, y + p.size * 0.4f,
                        x - p.size * 0.6f, y - p.size * 0.6f,
                        x, y - p.size
                    )
                    close()
                }
                drawPath(path = path, color = p.baseColor.copy(alpha = 0.65f))
            }
        }
    }
}

private class Butterfly(
    val initialX: Float,
    val initialY: Float,
    val scale: Float,
    val flapSpeed: Float,
    val pathRadiusX: Float,
    val pathRadiusY: Float,
    val speedAngle: Float,
    val color: Color
)

@Composable
private fun ButterfliesCanvas(progress: Float) {
    val butterflies = remember {
        val colors = listOf(
            Color(0xFF38BDF8), // Cyan
            Color(0xFFA855F7), // Lavender
            Color(0xFFFBBF24), // Gold
            Color(0xFF34D399), // Emerald
            Color(0xFFFB7185)  // Rose
        )
        listOf(
            Butterfly(0.20f, 0.35f, 1.1f, 18f, 70f, 25f, 1.0f, colors[0]),
            Butterfly(0.75f, 0.40f, 1.0f, 22f, 85f, 30f, 0.8f, colors[1]),
            Butterfly(0.48f, 0.65f, 0.9f, 20f, 60f, 20f, 1.2f, colors[2]),
            Butterfly(0.85f, 0.70f, 0.85f, 24f, 50f, 18f, 0.9f, colors[3]),
            Butterfly(0.12f, 0.75f, 0.95f, 19f, 65f, 22f, 1.1f, colors[4])
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        butterflies.forEachIndexed { idx, b ->
            val angle = (progress * b.speedAngle * 2f * Math.PI.toFloat()) + idx
            val posX = (b.initialX * w) + cos(angle) * b.pathRadiusX
            val posY = (b.initialY * h) + sin(angle * 1.5f) * b.pathRadiusY

            // Flap cycle (-1 to 1)
            val flap = cos(progress * b.flapSpeed * 2f * Math.PI.toFloat() + idx)
            val wingWidthScale = 0.25f + 0.75f * kotlin.math.abs(flap)

            // Subtle glow circle
            drawCircle(
                color = b.color.copy(alpha = 0.22f),
                radius = 16f * b.scale,
                center = Offset(posX, posY)
            )

            // Draw Butterfly Wings
            drawButterflyWings(
                x = posX,
                y = posY,
                scale = b.scale,
                wingWidthScale = wingWidthScale,
                color = b.color
            )
        }
    }
}

private fun DrawScope.drawButterflyWings(
    x: Float,
    y: Float,
    scale: Float,
    wingWidthScale: Float,
    color: Color
) {
    val wingW = 12f * scale * wingWidthScale
    val wingH = 10f * scale

    // Left Wing
    val leftPath = Path().apply {
        moveTo(x, y)
        cubicTo(x - wingW * 1.2f, y - wingH * 1.2f, x - wingW * 1.4f, y - wingH * 0.3f, x, y)
        cubicTo(x - wingW * 0.9f, y + wingH * 0.3f, x - wingW * 0.8f, y + wingH * 0.8f, x, y)
        close()
    }
    drawPath(path = leftPath, color = color.copy(alpha = 0.78f))

    // Right Wing
    val rightPath = Path().apply {
        moveTo(x, y)
        cubicTo(x + wingW * 1.2f, y - wingH * 1.2f, x + wingW * 1.4f, y - wingH * 0.3f, x, y)
        cubicTo(x + wingW * 0.9f, y + wingH * 0.3f, x + wingW * 0.8f, y + wingH * 0.8f, x, y)
        close()
    }
    drawPath(path = rightPath, color = color.copy(alpha = 0.78f))

    // Body
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = 2f * scale,
        center = Offset(x, y)
    )
}

private class Star(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val pulseFreq: Float,
    val driftSpeed: Float,
    val color: Color
)

@Composable
private fun StarlightCanvas(progress: Float) {
    val stars = remember {
        val colors = listOf(
            Color(0xFFFDE047),
            Color(0xFF38BDF8),
            Color(0xFFA78BFA),
            Color(0xFFF472B6)
        )
        List(22) { i ->
            Star(
                xRatio = ((i * 17) % 100) / 100f,
                yRatio = ((i * 23) % 100) / 100f,
                radius = 3f + (i % 4) * 1.5f,
                pulseFreq = 3f + (i % 5) * 1.2f,
                driftSpeed = 0.12f + ((i * 3) % 5) * 0.04f,
                color = colors[i % colors.size]
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        stars.forEachIndexed { i, s ->
            val y = ((s.yRatio - progress * s.driftSpeed) % 1f + 1f) % 1f * h
            val x = (s.xRatio * w + sin(progress * 4f + i) * 10f) % w

            val pulse = (sin((progress * s.pulseFreq + i) * Math.PI.toFloat() * 2f) + 1f) / 2f
            val alpha = 0.2f + pulse * 0.65f

            // Radial soft glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(s.color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(x, y),
                    radius = s.radius * 3.5f
                ),
                radius = s.radius * 3.5f,
                center = Offset(x, y)
            )

            // Center spark
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = s.radius * 0.6f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun EmbersCanvas(progress: Float) {
    val embers = remember {
        List(20) { i ->
            Petal(
                startXRatio = ((i * 19) % 100) / 100f,
                startYRatio = ((i * 31) % 100) / 100f,
                speedY = -0.5f - (i % 5) * 0.08f,
                speedX = (sin(i.toDouble()) * 0.08f).toFloat(),
                swayAmp = 12f + (i % 4) * 6f,
                swayFreq = 3f + (i % 3),
                size = 3.5f + (i % 4) * 1.5f,
                rotationSpeed = 0f,
                baseColor = if (i % 3 == 0) Color(0xFFFF4D00) else if (i % 3 == 1) Color(0xFFFFAA00) else Color(0xFFFF2200)
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        embers.forEachIndexed { i, e ->
            val t = ((e.startYRatio + progress * e.speedY) % 1f + 1f) % 1f
            val y = t * h
            val x = (e.startXRatio * w + sin(progress * e.swayFreq + i) * e.swayAmp) % w
            val alpha = (1f - (1f - t) * (1f - t)).coerceIn(0.2f, 0.85f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(e.baseColor.copy(alpha = alpha), Color.Transparent),
                    center = Offset(x, y),
                    radius = e.size * 2.8f
                ),
                radius = e.size * 2.8f,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = e.size * 0.45f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun SnowCanvas(progress: Float) {
    val snow = remember {
        List(24) { i ->
            Petal(
                startXRatio = ((i * 13) % 100) / 100f,
                startYRatio = ((i * 27) % 100) / 100f,
                speedY = 0.35f + (i % 4) * 0.06f,
                speedX = -0.1f - (i % 3) * 0.03f,
                swayAmp = 14f + (i % 5) * 5f,
                swayFreq = 2.5f + (i % 4) * 0.5f,
                size = 3f + (i % 4) * 1.2f,
                rotationSpeed = 0f,
                baseColor = Color(0xFFE2E8F0)
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        snow.forEachIndexed { i, s ->
            val t = (s.startYRatio + progress * s.speedY) % 1f
            val y = t * h
            val x = (s.startXRatio * w + progress * s.speedX * w + sin(progress * s.swayFreq + i) * s.swayAmp)
                .let { ((it % w) + w) % w }

            drawCircle(
                color = s.baseColor.copy(alpha = 0.65f),
                radius = s.size,
                center = Offset(x, y)
            )
        }
    }
}
