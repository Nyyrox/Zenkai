package com.nyyrox.aniverse.ui.components.decorations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.decorations.AvatarFrameTheme
import com.nyyrox.aniverse.data.decorations.DecorationRarity
import com.nyyrox.aniverse.data.decorations.TextEffectStyle
import com.nyyrox.aniverse.data.decorations.ZenkaiDecorationCatalog
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * 1. CIRCULAR 3D AVATAR FRAME RENDERER
 * ══════════════════════════════════════════════════════════════════════════════
 * Renders an intricate 3D metallic/crystalline bezel with specular highlights,
 * ambient aura glow, and crown crest around the circular avatar.
 */
@Composable
fun CircularAvatarFrameRenderer(
    theme: AvatarFrameTheme,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { theme.borderThicknessDp.dp.toPx() }

    // Subtle breathing pulse for the ambient aura
    val infiniteTransition = rememberInfiniteTransition(label = "FramePulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Subtle 3D specular highlight rotation
    val angleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpecularAngle"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Outer Aura Glow Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (this.size.minDimension / 2f) - (strokeWidthPx / 2f)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer soft aura
            drawCircle(
                color = theme.outerGlowColor.copy(alpha = glowAlpha * 0.45f),
                radius = radius + strokeWidthPx * 1.5f,
                center = center
            )
        }

        // 2. Avatar Content (inset by stroke width to fit snugly inside the frame)
        val insetPadding = (theme.borderThicknessDp + 2f).dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insetPadding)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }

        // 3. 3D Metallic Ring Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (this.size.minDimension / 2f) - (strokeWidthPx / 2f)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val arcSize = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // 3A. Dark inner bevel shadow for physical depth
            drawCircle(
                color = theme.innerBevelColor.copy(alpha = 0.85f),
                radius = radius - (strokeWidthPx * 0.4f),
                center = center,
                style = Stroke(width = strokeWidthPx * 0.5f)
            )

            // 3B. 3D Metallic Multi-Tone Rim (Sweep Gradient)
            val sweepBrush = Brush.sweepGradient(
                colors = theme.rimColors,
                center = center
            )
            drawCircle(
                brush = sweepBrush,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // 3C. Specular Reflection Arc (gleam of light across top quadrant)
            drawArc(
                color = theme.specularHighlight.copy(alpha = 0.7f),
                startAngle = -120f + (angleOffset * 0.15f),
                sweepAngle = 70f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx * 0.65f, cap = StrokeCap.Round)
            )

            // 3D. Secondary subtle reflection arc opposite
            drawArc(
                color = theme.specularHighlight.copy(alpha = 0.35f),
                startAngle = 45f + (angleOffset * 0.15f),
                sweepAngle = 40f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx * 0.45f, cap = StrokeCap.Round)
            )
        }

        // 4. Crown/Crest Accent at top edge of ring
        if (theme.crestEmoji.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1118))
                    .border(1.dp, theme.accentColor.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = theme.crestEmoji,
                    fontSize = (size.value * 0.16f).coerceIn(10f, 18f).sp
                )
            }
        }
    }
}

const val AVATAR_DECORATIONS_BASE_URL =
    "https://raw.githubusercontent.com/Nyyrox/SillyTavern-AvatarDecorations-CSS/main/dc-decorations/"

fun getAvatarDecorationUrl(fileName: String): String {
    val encoded = fileName.replace(" ", "%20")
    return "$AVATAR_DECORATIONS_BASE_URL$encoded"
}

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * 2. MASTER AVATAR DECORATION RENDERER
 * ══════════════════════════════════════════════════════════════════════════════
 * Renders the circular avatar with an animated Discord overlay loaded directly
 * from GitHub repo (https://github.com/Nyyrox/SillyTavern-AvatarDecorations-CSS).
 * Overlays are zoomed (1.55x) so they comfortably frame and fill the avatar.
 */
@Composable
fun AvatarDecorationRenderer(
    decorationId: String? = null,
    frameId: String? = null, // Deprecated parameter kept for compatibility
    size: Dp = 72.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val overlayItem = remember(decorationId) {
        val oid = decorationId?.takeIf { !it.startsWith("cf_") }
        oid?.let { ZenkaiDecorationCatalog.findById(it) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Base Avatar (Strictly circular)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }

        // Overlay Animated APNG (1.34x scale, unconstrained so ears/horns/masks fit comfortably OUTSIDE the avatar circle)
        if (overlayItem != null && !overlayItem.assetFileName.isNullOrBlank()) {
            val overlayUrl = getAvatarDecorationUrl(overlayItem.assetFileName)
            ApngDecorationImage(
                url = overlayUrl,
                contentDescription = overlayItem.name,
                modifier = Modifier
                    .wrapContentSize(align = Alignment.Center, unbounded = true)
                    .requiredSize(size * 1.34f)
            )
        }
    }
}

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * 3. USERNAME TEXT EFFECT RENDERER (16 Masterpiece 60fps Kinetic & Shader Effects)
 * ══════════════════════════════════════════════════════════════════════════════
 */
@Composable
fun TextEffectRenderer(
    effectId: String?,
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val item = remember(effectId) {
        effectId?.let { ZenkaiDecorationCatalog.findById(it) }
    }
    val theme = item?.textEffectTheme

    if (theme == null) {
        Text(
            text = text,
            modifier = modifier,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = Color.White,
            maxLines = maxLines,
            overflow = overflow,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(1f, 2f),
                    blurRadius = 4f
                )
            )
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "TextEffect_${theme.effectStyle}")

    when (theme.effectStyle) {
        TextEffectStyle.RGB_GLITCH -> {
            // Smooth chromatic aberration with dynamic red & cyan micro-jitter
            val jitterPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6.28318f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "GlitchJitter"
            )
            val jitterX = (sin(jitterPhase.toDouble()) * 2.2f).toFloat()
            val glitchAlpha = 0.85f + (sin((jitterPhase * 3f).toDouble()) * 0.15f).toFloat()

            Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
                // Cyan chromatic split
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = Color(0xFF00F0FF).copy(alpha = glitchAlpha * 0.75f),
                    maxLines = maxLines,
                    overflow = overflow,
                    modifier = Modifier.offset(x = (-jitterX).dp)
                )
                // Red/Magenta chromatic split
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = Color(0xFFFF0055).copy(alpha = glitchAlpha * 0.75f),
                    maxLines = maxLines,
                    overflow = overflow,
                    modifier = Modifier.offset(x = jitterX.dp)
                )
                // Crisp White Base Layer
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = Color.White,
                    maxLines = maxLines,
                    overflow = overflow,
                    style = TextStyle(
                        shadow = Shadow(
                            color = theme.glowColor.copy(alpha = 0.8f),
                            blurRadius = theme.glowRadius
                        )
                    )
                )
            }
        }

        TextEffectStyle.SHIMMER -> {
            val progress by infiniteTransition.animateFloat(
                initialValue = -0.5f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ShimmerProgress"
            )
            val sweepWidth = 250f
            val startX = progress * 600f - sweepWidth
            val endX = startX + sweepWidth
            val brush = Brush.linearGradient(
                colors = theme.primaryColors,
                start = Offset(startX, 0f),
                end = Offset(endX, 60f)
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = theme.glowColor.copy(alpha = 0.6f),
                        blurRadius = theme.glowRadius
                    )
                )
            )
        }

        TextEffectStyle.BOUNCE -> {
            val bouncePhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6.28318f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "BouncePhase"
            )
            // Snappy parabolic spring bounce with squash and stretch
            val rawSin = sin(bouncePhase.toDouble()).toFloat()
            val bounceY = -abs(rawSin) * 8.0f
            val squashX = 1f + (abs(bounceY) * 0.035f)
            val stretchY = 1f - (abs(bounceY) * 0.04f)
            val brush = Brush.linearGradient(theme.primaryColors)
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier.graphicsLayer {
                    translationY = bounceY * density
                    scaleX = squashX
                    scaleY = stretchY
                },
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = theme.glowColor.copy(alpha = 0.85f),
                        offset = Offset(0f, 4f + abs(bounceY)),
                        blurRadius = theme.glowRadius
                    )
                )
            )
        }

        TextEffectStyle.PULSE -> {
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.97f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs / 2, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseScale"
            )
            val brush = Brush.linearGradient(theme.primaryColors)
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier.graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = theme.glowColor.copy(alpha = (pulseScale - 0.95f) * 7f),
                        blurRadius = theme.glowRadius * pulseScale
                    )
                )
            )
        }

        TextEffectStyle.HEARTBEAT -> {
            val beatProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "HeartbeatProgress"
            )
            val scale = when {
                beatProgress in 0.0f..0.12f -> 1.0f + (sin(beatProgress / 0.12f * Math.PI) * 0.14f).toFloat()
                beatProgress in 0.18f..0.32f -> 1.0f + (sin((beatProgress - 0.18f) / 0.14f * Math.PI) * 0.18f).toFloat()
                else -> 1.0f
            }
            val glowIntensity = if (scale > 1.02f) 0.95f else 0.45f
            val brush = Brush.linearGradient(theme.primaryColors)
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = theme.glowColor.copy(alpha = glowIntensity),
                        blurRadius = theme.glowRadius * scale
                    )
                )
            )
        }

        TextEffectStyle.FLICKER -> {
            val flickerPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "FlickerPhase"
            )
            val alpha = when {
                flickerPhase in 0.20f..0.24f -> 0.45f
                flickerPhase in 0.28f..0.30f -> 0.70f
                flickerPhase in 0.72f..0.76f -> 0.50f
                flickerPhase in 0.82f..0.85f -> 0.65f
                else -> 0.98f
            }
            val brush = Brush.linearGradient(theme.primaryColors)
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier.graphicsLayer { this.alpha = alpha },
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = theme.glowColor.copy(alpha = alpha * 0.8f),
                        blurRadius = theme.glowRadius
                    )
                )
            )
        }

        TextEffectStyle.THUNDERSTORM -> {
            val thunderPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ThunderPhase"
            )
            val isFlash = (thunderPhase in 0.12f..0.16f) || (thunderPhase in 0.19f..0.22f) || (thunderPhase in 0.85f..0.88f)
            val brushColors = if (isFlash) listOf(Color.White, Color(0xFFFDE047), Color.White) else theme.primaryColors
            val brush = Brush.linearGradient(brushColors)
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = if (isFlash) Color.White else theme.glowColor,
                        blurRadius = if (isFlash) theme.glowRadius * 2.0f else theme.glowRadius
                    )
                )
            )
        }

        TextEffectStyle.MOLTEN -> {
            // Volcanic magma heat distortion with bubbling ember pulse and fiery shifting core
            val magmaProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "MoltenProgress"
            )
            val heatDistort = (sin(magmaProgress * 6.28318f * 3f) * 1.5f).toFloat()
            val glowPulse = 14f + (sin(magmaProgress * 6.28318f * 2f) * 8f).toFloat()
            val shift = magmaProgress * 300f
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFDC2626), // Deep Crimson
                    Color(0xFFEA580C), // Molten Orange
                    Color(0xFFF59E0B), // Lava Gold
                    Color(0xFFFEF08A), // White-Hot Core
                    Color(0xFFEA580C),
                    Color(0xFFDC2626)
                ),
                start = Offset(shift, 0f),
                end = Offset(shift + 180f, 60f)
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier.graphicsLayer {
                    translationY = heatDistort * 0.5f
                },
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFFFF4500).copy(alpha = 0.9f),
                        blurRadius = glowPulse
                    )
                )
            )
        }

        TextEffectStyle.FROSTBITE -> {
            // Glacial crystalline shimmer with sub-zero cyan sparkles and breathing freeze mist
            val frostProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "FrostProgress"
            )
            val sparkleX = frostProgress * 400f - 100f
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0284C7), // Glacier Blue
                    Color(0xFF38BDF8), // Ice Cyan
                    Color.White,       // Pure Frost Diamond
                    Color(0xFF06B6D4), // Teal Crystal
                    Color(0xFF0284C7)
                ),
                start = Offset(sparkleX, 0f),
                end = Offset(sparkleX + 160f, 40f)
            )
            val auraPulse = 12f + (sin(frostProgress * 6.28318f) * 6f).toFloat()
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                        blurRadius = auraPulse
                    )
                )
            )
        }

        TextEffectStyle.NEON_PRISM -> {
            // High-speed chromatic rainbow spectrum with rotating radiant edge glow
            val prismProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "PrismProgress"
            )
            val prismOffset = prismProgress * 500f
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF007F), // Neon Magenta
                    Color(0xFF7928CA), // Electric Purple
                    Color(0xFF0070F3), // Cyan Blue
                    Color(0xFF00DFD8), // Aquamarine
                    Color(0xFFFF007F)  // Neon Magenta
                ),
                start = Offset(prismOffset, 0f),
                end = Offset(prismOffset + 220f, 50f)
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFFFF007F).copy(alpha = 0.85f),
                        blurRadius = 18f
                    )
                )
            )
        }

        TextEffectStyle.FLOW -> {
            // Liquid mercury & deep oceanic laminar wave rolling across letters
            val flowProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6.28318f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "FlowProgress"
            )
            val xShift = (sin(flowProgress.toDouble()) * 180f).toFloat()
            val yShift = (cos(flowProgress.toDouble()) * 30f).toFloat()
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0D9488), Color(0xFF14B8A6), Color(0xFF5EEAD4),
                    Color(0xFF06B6D4), Color(0xFF0D9488)
                ),
                start = Offset(xShift, yShift),
                end = Offset(xShift + 250f, yShift + 60f)
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFF14B8A6).copy(alpha = 0.8f),
                        blurRadius = 14f
                    )
                )
            )
        }

        TextEffectStyle.PHANTOM -> {
            // Ghostly eerie ethereal spirit drift: floating translation and misty alpha wave
            val phantomProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6.28318f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "PhantomProgress"
            )
            val driftY = (sin(phantomProgress.toDouble()) * 3.0).toFloat()
            val driftX = (cos(phantomProgress.toDouble() * 0.5) * 2.0).toFloat()
            val ghostAlpha = 0.70f + (sin(phantomProgress.toDouble() * 2.0) * 0.25f).toFloat()
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8B5CF6).copy(alpha = ghostAlpha),
                    Color(0xFFC084FC).copy(alpha = ghostAlpha),
                    Color(0xFFDDD6FE).copy(alpha = ghostAlpha)
                )
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier.graphicsLayer {
                    translationY = driftY * density
                    translationX = driftX * density
                },
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFFA855F7).copy(alpha = ghostAlpha * 0.9f),
                        blurRadius = 16f
                    )
                )
            )
        }

        TextEffectStyle.ELECTRIC -> {
            // High-voltage erratic micro-jitter with plasma electric blue and lightning arc flashes
            val electricPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ElectricPhase"
            )
            val isDischarge = (electricPhase in 0.15f..0.22f) || (electricPhase in 0.58f..0.65f) || (electricPhase in 0.88f..0.94f)
            val jitterX = if (isDischarge) ((electricPhase * 100f) % 4f - 2f) else 0f
            val brushColors = if (isDischarge) {
                listOf(Color.White, Color(0xFF38BDF8), Color.White)
            } else {
                listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF67E8F9))
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier.graphicsLayer { translationX = jitterX * density },
                style = TextStyle(
                    brush = Brush.linearGradient(brushColors),
                    shadow = Shadow(
                        color = if (isDischarge) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        blurRadius = if (isDischarge) 22f else 12f
                    )
                )
            )
        }

        TextEffectStyle.STARFALL -> {
            // Shooting stars and twinkling cosmic golden meteor streaks
            val starProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "StarfallProgress"
            )
            val sweep = starProgress * 450f - 100f
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFD97706),
                    Color(0xFFFBBF24),
                    Color(0xFFFEF3C7),
                    Color.White,
                    Color(0xFFFBBF24),
                    Color(0xFFD97706)
                ),
                start = Offset(sweep, sweep * 0.5f),
                end = Offset(sweep + 160f, sweep * 0.5f + 50f)
            )
            val twinkleGlow = 12f + (sin(starProgress * 6.28318f * 3f) * 6f).toFloat()
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFFFBBF24).copy(alpha = 0.85f),
                        blurRadius = twinkleGlow
                    )
                )
            )
        }

        TextEffectStyle.AURORA -> {
            // Northern lights undulating curtains: emerald green, teal, celestial violet
            val auroraProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6.28318f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "AuroraProgress"
            )
            val waveX = (sin(auroraProgress.toDouble()) * 200f).toFloat()
            val waveY = (cos(auroraProgress.toDouble() * 1.5) * 35f).toFloat()
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF10B981), // Emerald Aurora
                    Color(0xFF06B6D4), // Cyan Glow
                    Color(0xFF8B5CF6), // Violet Sky
                    Color(0xFF10B981)
                ),
                start = Offset(waveX, waveY),
                end = Offset(waveX + 300f, waveY + 80f)
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFF10B981).copy(alpha = 0.85f),
                        blurRadius = 18f
                    )
                )
            )
        }

        TextEffectStyle.SUPERNOVA -> {
            // Cosmic stellar explosion: pulsing stellar core, radiant shockwave flare
            val novaProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "SupernovaProgress"
            )
            val novaScale = 1.0f + (sin(novaProgress * 3.14159f) * 0.08f).toFloat()
            val flareSweep = novaProgress * 350f
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF43F5E), Color(0xFFFB923C), Color(0xFFFEF08A),
                    Color(0xFFFFFFFF), Color(0xFFFB923C), Color(0xFFF43F5E)
                ),
                start = Offset(flareSweep, 0f),
                end = Offset(flareSweep + 200f, 60f)
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier.graphicsLayer {
                    scaleX = novaScale
                    scaleY = novaScale
                },
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFFF43F5E).copy(alpha = 0.9f),
                        blurRadius = 14f * novaScale
                    )
                )
            )
        }

        TextEffectStyle.ECLIPSE -> {
            // Solar eclipse: dark mysterious silhouette text with dazzling golden coronal rim burning around edges
            val eclipseProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6.28318f,
                animationSpec = infiniteRepeatable(
                    animation = tween(theme.animationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "EclipseProgress"
            )
            val ringAngle = eclipseProgress
            val coronaGlow = 14f + (sin(ringAngle * 2.0) * 8f).toFloat()
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFFF59E0B), Color(0xFF0F172A), Color(0xFF1E293B)
                )
            )
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                modifier = modifier,
                style = TextStyle(
                    brush = brush,
                    shadow = Shadow(
                        color = Color(0xFFF59E0B).copy(alpha = 0.95f),
                        blurRadius = coronaGlow
                    )
                )
            )
        }
    }
}

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * 4. NAMEPLATE RENDERER (Backward-Compatible Wrapper)
 * ══════════════════════════════════════════════════════════════════════════════
 */
@Composable
fun NameplateRenderer(
    nameplateId: String?,
    customEmoji: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * 4. RARITY BADGE CHIP
 * ══════════════════════════════════════════════════════════════════════════════
 */
@Composable
fun RarityBadge(rarity: DecorationRarity, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(rarity.color.copy(alpha = 0.16f))
            .border(0.5.dp, rarity.color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(rarity.color)
        )
        Text(
            text = rarity.label.uppercase(),
            color = rarity.color,
            fontSize = 9.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * 5. PROFILE BANNER RENDERER (Clean Base Banner)
 * ══════════════════════════════════════════════════════════════════════════════
 */
@Composable
fun ProfileBannerRenderer(
    customBannerUrl: String? = null,
    modifier: Modifier = Modifier,
    bannerThemeId: String? = null
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E2330), Color(0xFF10131B))
                )
            )
    ) {
        if (!customBannerUrl.isNullOrBlank()) {
            AsyncImage(
                model = customBannerUrl,
                contentDescription = "Profile Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
