package com.nyyrox.aniverse.ui.components

import android.content.Context
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private data class StarParticle(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val phaseOffset: Float,
    val hasSpikes: Boolean
)

@Composable
fun ZenkaiCommunityCard(
    modifier: Modifier = Modifier,
    telegramUrl: String = "https://t.me/StreamverseOfc",
    discordUrl: String = "https://discord.gg/aXTfS6XGg"
) {
    // Generate fixed random star distribution
    val stars = remember {
        val list = mutableListOf<StarParticle>()
        val rnd = java.util.Random(1337)
        repeat(24) { i ->
            list.add(
                StarParticle(
                    relX = rnd.nextFloat(),
                    relY = rnd.nextFloat(),
                    radius = 1.2f + rnd.nextFloat() * 2.2f,
                    phaseOffset = rnd.nextFloat() * (2 * PI.toFloat()),
                    hasSpikes = i % 4 == 0
                )
            )
        }
        list
    }

    // Slow organic twinkle cycle (4.8 seconds)
    val infiniteTransition = rememberInfiniteTransition(label = "stars_twinkle")
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle_phase"
    )

    // Highlighted border pulse
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-10).dp)
            .clip(RoundedCornerShape(22.dp))
            .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x60FF1E27))
            .border(
                width = 1.3.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFF1E27).copy(alpha = borderPulse),
                        OttInk(0.44f).copy(alpha = borderPulse),
                        Color(0xFFFF1E27).copy(alpha = borderPulse)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── 1. Root Video Background (zenkai_bg.mp4) ───────────────────────────
        AndroidView(
            factory = { ctx ->
                object : FrameLayout(ctx) {
                    init {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        clipChildren = true

                        val textureView = TextureView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                var mediaPlayer: MediaPlayer? = null

                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                    try {
                                        val uri = Uri.parse("android.resource://" + ctx.packageName + "/" + R.raw.zenkai_bg)
                                        mediaPlayer = MediaPlayer().apply {
                                            setDataSource(ctx, uri)
                                            setSurface(Surface(st))
                                            isLooping = true
                                            setVolume(0f, 0f)
                                            setOnPreparedListener { mp -> mp.start() }
                                            prepareAsync()
                                        }
                                    } catch (_: Throwable) {}
                                }

                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                    try {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                    } catch (_: Throwable) {}
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                        }
                        addView(textureView)
                    }
                }
            },
            modifier = Modifier.matchParentSize()
        )

        // ── 2. Cinematic Gradient Overlay (Ruby & Pitch Dark) ───────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xD9100407),
                            Color(0x990B0205),
                            Color(0xF2070104)
                        )
                    )
                )
        )

        // ── 3. Twinkling Star Particles Overlay ─────────────────────────────────
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            stars.forEach { star ->
                val px = star.relX * w
                val py = star.relY * h
                // Organic sine wave twinkle with distinct star phase
                val alpha = (0.20f + 0.80f * sin(twinklePhase + star.phaseOffset).pow(2)).coerceIn(0.12f, 1f)
                val starColor = Color(0xFFFFF0F5).copy(alpha = alpha)

                // Soft outer star glow
                drawCircle(
                    color = Color(0x60FF3B49).copy(alpha = alpha * 0.45f),
                    radius = star.radius * 2.8f,
                    center = Offset(px, py)
                )
                // Core bright star dot
                drawCircle(
                    color = starColor,
                    radius = star.radius,
                    center = Offset(px, py)
                )

                // 4-point twinkle cross rays for bright stars
                if (star.hasSpikes && alpha > 0.55f) {
                    val rayLen = star.radius * 3.5f * alpha
                    val rayColor = Color.White.copy(alpha = alpha * 0.70f)
                    drawLine(
                        color = rayColor,
                        start = Offset(px - rayLen, py),
                        end = Offset(px + rayLen, py),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = rayColor,
                        start = Offset(px, py - rayLen),
                        end = Offset(px, py + rayLen),
                        strokeWidth = 1f
                    )
                }
            }
        }

        // ── 4. Card Content (Horizontal Landscape Orientation) ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: App Logo + Title + Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "ZENKAI Logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0x90FF1E27), CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column {
                    Text(
                        text = "ZENKAI",
                        fontFamily = ZenkaiFontFamily,
                        fontSize = 22.sp,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Black,
                        color = OttTextPrimary
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        modifier = Modifier
                            .clip(OttPillShape)
                            .background(Color(0x30FF1E27))
                            .border(0.6.dp, Color(0x60FF1E27), OttPillShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFF4D55),
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(Modifier.size(3.dp))
                        Text(
                            text = "OFFICIAL COMMUNITY",
                            fontFamily = ZenkaiFontFamily,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF0F3FA)
                        )
                    }
                }
            }

            // Right Side: Telegram & Discord Buttons
            ZenkaiCommunityRow(
                telegramUrl = telegramUrl,
                discordUrl = discordUrl
            )
        }
    }
}