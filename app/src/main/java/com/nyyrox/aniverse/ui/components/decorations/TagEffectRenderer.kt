package com.nyyrox.aniverse.ui.components.decorations

import android.widget.ImageView
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.loader.StreamLoader
import java.io.InputStream
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-Quality Luxury OWNER Badge with Custom Golden Crown Vector, Shimmer Sweep, and Cosmic Gold Aura.
 * Completely emoji-free.
 */
@Composable
fun LuxuryOwnerBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "owner_badge")
    val sweepX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "owner_sweep"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "owner_glow"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFFFFD700).copy(alpha = glowAlpha))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF261803),
                        Color(0xFF663806),
                        Color(0xFFB45309),
                        Color(0xFFF59E0B),
                        Color(0xFF261803)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFD97706).copy(alpha = glowAlpha),
                        Color(0xFFFFFAF0),
                        Color(0xFFFFD700).copy(alpha = glowAlpha)
                    )
                ),
                RoundedCornerShape(6.dp)
            )
            .drawBehind {
                val width = size.width
                val height = size.height
                val startX = width * sweepX
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x66FFFFFF),
                            Color(0x99FFD700),
                            Color.Transparent
                        ),
                        start = Offset(startX - 20f, 0f),
                        end = Offset(startX + 40f, height)
                    )
                )
            }
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Handcrafted Royal Crown Vector (No emoji)
            Canvas(modifier = Modifier.size(10.dp, 8.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.35f)
                    lineTo(w * 0.25f, h * 0.65f)
                    lineTo(w * 0.5f, 0f)
                    lineTo(w * 0.75f, h * 0.65f)
                    lineTo(w, h * 0.35f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFFBEB), Color(0xFFFFD700), Color(0xFFD97706))
                    ),
                    style = Fill
                )
            }
            Spacer(modifier = Modifier.width(3.5.dp))
            Text(
                text = "OWNER",
                color = Color(0xFFFFFBEB),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xCC000000),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                )
            )
        }
    }
}

/**
 * Fiery Crimson ADMIN Badge with Ruby Shield Vector.
 * Completely emoji-free.
 */
@Composable
fun AdminBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "admin_badge")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "admin_glow"
    )

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFFE11D48).copy(alpha = glowAlpha))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF4C0519),
                        Color(0xFF9F1239),
                        Color(0xFFE11D48),
                        Color(0xFF4C0519)
                    )
                )
            )
            .border(
                1.dp,
                Color(0xFFFB7185).copy(alpha = glowAlpha),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Handcrafted Shield Vector
            Canvas(modifier = Modifier.size(8.dp, 9.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    lineTo(w, h * 0.25f)
                    lineTo(w * 0.85f, h * 0.75f)
                    lineTo(w * 0.5f, h)
                    lineTo(w * 0.15f, h * 0.75f)
                    lineTo(0f, h * 0.25f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFFB7185), Color(0xFFBE123C))
                    ),
                    style = Fill
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "ADMIN",
                color = Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xAA000000),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                )
            )
        }
    }
}

/**
 * Jade Blade MODERATOR Badge with Emerald Glow.
 * Completely emoji-free.
 */
@Composable
fun ModeratorBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mod_badge")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mod_glow"
    )

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFF10B981).copy(alpha = glowAlpha))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF064E3B),
                        Color(0xFF047857),
                        Color(0xFF10B981),
                        Color(0xFF064E3B)
                    )
                )
            )
            .border(
                1.dp,
                Color(0xFF34D399).copy(alpha = glowAlpha),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(8.dp, 8.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    lineTo(w, h * 0.5f)
                    lineTo(w * 0.5f, h)
                    lineTo(0f, h * 0.5f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFD1FAE5), Color(0xFF34D399), Color(0xFF047857))
                    ),
                    style = Fill
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "MOD",
                color = Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp
            )
        }
    }
}

/**
 * Composite Multi-Role Row supporting OWNER, ADMIN, MODERATOR concurrently.
 */
@Composable
fun RoleBadgeRow(
    roles: List<String>?,
    isAdmin: Boolean? = false,
    modifier: Modifier = Modifier
) {
    val roleSet = (roles ?: emptyList()).map { it.uppercase().trim() }.toSet()
    val isOwner = roleSet.contains("OWNER")
    val hasAdmin = roleSet.contains("ADMIN") || (isAdmin == true)
    val isMod = roleSet.contains("MODERATOR") || roleSet.contains("MOD")

    if (!isOwner && !hasAdmin && !isMod) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOwner) LuxuryOwnerBadge()
        if (hasAdmin) AdminBadge()
        if (isMod) ModeratorBadge()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BESPOKE TAG NAMEPLATES (Custom Transparent PNGs, Shaders, Zero Emojis)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * STRAW HAT Nameplate: Clean transparent PNG of Luffy's Straw Hat with Floating Golden Starlight Particles.
 */
@Composable
fun StrawHatNameplate(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "straw_hat")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFFEAB308).copy(alpha = 0.6f))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF261803), Color(0xFF451A03), Color(0xFF78350F), Color(0xFF261803))
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFF59E0B))),
                RoundedCornerShape(6.dp)
            )
            .drawBehind {
                // Draw 3 floating starlight particles around the hat
                for (i in 0..2) {
                    val angle = phase + (i * 2.094f)
                    val px = size.width * 0.2f + (cos(angle) * 10f)
                    val py = size.height * 0.5f + (sin(angle) * 5f)
                    drawCircle(
                        color = Color(0xFFFDE047).copy(alpha = (0.5f + 0.5f * sin(angle)).coerceIn(0.2f, 1f)),
                        radius = 1.6f,
                        center = Offset(px, py)
                    )
                }
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_straw_hat),
                contentDescription = "Straw Hat",
                modifier = Modifier.size(18.dp, 13.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "STRAW HAT",
                color = Color(0xFFFEF08A),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF7F1D1D),
                        offset = Offset(0.5f, 0.5f),
                        blurRadius = 2f
                    )
                )
            )
        }
    }
}

/**
 * ANBU Nameplate: Transparent PNG of the ANBU Fox Porcelain Mask with Sharp Red Edge Accent.
 */
@Composable
fun AnbuNameplate(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "anbu")
    val stealthGlow by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stealth"
    )

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFFE2E8F0).copy(alpha = stealthGlow))
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF090D16))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFBE123C), Color(0xFFE2E8F0))),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_anbu_mask),
                contentDescription = "ANBU Mask",
                modifier = Modifier.size(14.dp, 18.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "ANBU",
                color = Color(0xFFF8FAFC),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFBE123C),
                        offset = Offset(0.5f, 0.5f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}

/**
 * HASHIRA Nameplate: Transparent PNG of Crossed Nichirin Katana Swords with Flame & Water Embers.
 */
@Composable
fun HashiraNameplate(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hashira")
    val bladeSheen by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFF38BDF8).copy(alpha = 0.5f))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1E1035), Color(0xFF3B0764), Color(0xFF1E1035))
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFF38BDF8), Color(0xFFEC4899))),
                RoundedCornerShape(6.dp)
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val startX = w * bladeSheen
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0x77FFFFFF), Color.Transparent),
                        start = Offset(startX - 10f, 0f),
                        end = Offset(startX + 18f, h)
                    )
                )
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_hashira),
                contentDescription = "Hashira Swords",
                modifier = Modifier.size(17.dp, 17.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "HASHIRA",
                color = Color(0xFFFDF2F8),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * JUJUTSU SORCERER Nameplate: Transparent PNG of the official Kanji emblem with Cursed Energy Flames.
 */
@Composable
fun SorcererNameplate(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sorcerer")
    val cursedPulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursed"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFFA855F7).copy(alpha = cursedPulse))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF130421), Color(0xFF2E1065), Color(0xFF130421))
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFF7C3AED), Color(0xFFC084FC).copy(alpha = cursedPulse), Color(0xFF7C3AED))
                ),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_jujutsu),
                contentDescription = "Jujutsu Kaisen",
                modifier = Modifier.height(13.dp),
                contentScale = ContentScale.FillHeight
            )
            Text(
                text = "SORCERER",
                color = Color(0xFFE9D5FF),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/**
 * INFERNO Nameplate: Volcanic Molten Magma & Heat Distortion (No Emojis).
 */
@Composable
fun InfernoNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "inferno")
    val heatPulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heat"
    )
    val emberSweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ember"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(4.dp), spotColor = Color(0xFFDC2626).copy(alpha = heatPulse))
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF260505),
                        Color(0xFF450A0A),
                        Color(0xFF7F1D1D),
                        Color(0xFF991B1B),
                        Color(0xFF260505)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFEA580C),
                        Color(0xFFEF4444),
                        Color(0xFFFBBF24).copy(alpha = heatPulse),
                        Color(0xFFEA580C)
                    )
                ),
                RoundedCornerShape(4.dp)
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val startX = w * emberSweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0x66F97316), Color(0x99FBBF24), Color.Transparent),
                        start = Offset(startX - 15f, 0f),
                        end = Offset(startX + 25f, h)
                    )
                )
            }
            .padding(horizontal = 8.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.uppercase(),
            color = Color(0xFFFED7AA),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xFF7F1D1D),
                    offset = Offset(0f, 0f),
                    blurRadius = 6f
                )
            )
        )
    }
}

/**
 * BLAZE Nameplate: Sunfire Crimson & Amber Solar Flares.
 */
@Composable
fun BlazeNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "blaze")
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blaze_pulse"
    )

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFFF97316).copy(alpha = pulse))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF331405), Color(0xFF7C2D12), Color(0xFFC2410C), Color(0xFF331405))
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFFF97316).copy(alpha = pulse), Color(0xFFFDE047), Color(0xFFF97316).copy(alpha = pulse))
                ),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.uppercase(),
            color = Color(0xFFFEF08A),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
    }
}

/**
 * SPARK Nameplate: Bronze and Ember Accent.
 */
@Composable
fun SparkNameplate(tag: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF26150A), Color(0xFF4A2511), Color(0xFF26150A))
                )
            )
            .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.6f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.uppercase(),
            color = Color(0xFFFDBA74),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}


/**
 * GENIN Nameplate: Authentic Shinobi Forehead Protector Steel Plate with Metallic Rivets and Leaf Crest PNG.
 */
@Composable
fun GeninNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "genin")
    val sheenX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "genin_sheen"
    )

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(4.dp), spotColor = Color(0xFF64748B))
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF334155),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF334155)
                    )
                )
            )
            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp))
            .drawBehind {
                val w = size.width
                val h = size.height
                val startX = w * sheenX
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0x35FFFFFF), Color.Transparent),
                        start = Offset(startX - 10f, 0f),
                        end = Offset(startX + 20f, h)
                    )
                )
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_genin),
                contentDescription = "Genin Protector",
                modifier = Modifier.size(19.dp, 13.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = tag.uppercase(),
                color = Color(0xFFF8FAFC),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF020617),
                        offset = Offset(0.8f, 0.8f),
                        blurRadius = 1.5f
                    )
                )
            )
        }
    }
}

/**
 * CHUNIN Nameplate: Tactical Shinobi Flak Vest Emerald Steel with Kunai Dagger PNG.
 */
@Composable
fun ChuninNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "chunin")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chunin_glow"
    )

    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFF10B981).copy(alpha = pulse))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF047857), Color(0xFF064E3B))
                )
            )
            .border(1.dp, Color(0xFF34D399).copy(alpha = pulse), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_chunin),
                contentDescription = "Chunin Kunai",
                modifier = Modifier.size(9.dp, 16.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color(0xFFA7F3D0),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp
            )
        }
    }
}

/**
 * JONIN Nameplate: Elite Deep Cobalt Shinobi Plate with Crossed Shuriken PNG & Electric Edge.
 */
@Composable
fun JoninNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "jonin")
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jonin_pulse"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFF38BDF8).copy(alpha = pulse))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0C2461), Color(0xFF1E3799), Color(0xFF1E40AF), Color(0xFF0C2461))
                )
            )
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = pulse), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_jonin),
                contentDescription = "Jonin Shuriken",
                modifier = Modifier.size(16.dp, 16.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color(0xFFBAE6FD),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/**
 * DEMON SLAYER Nameplate: Nichirin Steel Blade Plate with Hanafuda Earring PNG & Sheen Sweep.
 */
@Composable
fun DemonSlayerNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "slayer")
    val sweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blade_sweep"
    )
    val flamePulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFFEF4444).copy(alpha = flamePulse))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1C1917), Color(0xFF292524), Color(0xFF450A0A), Color(0xFF1C1917))
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFFCA5A5), Color(0xFFB91C1C))),
                RoundedCornerShape(5.dp)
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val startX = w * sweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0x77FFFFFF), Color.Transparent),
                        start = Offset(startX - 10f, 0f),
                        end = Offset(startX + 18f, h)
                    )
                )
            }
            .padding(horizontal = 6.5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_demon_slayer),
                contentDescription = "Demon Slayer",
                modifier = Modifier.size(16.dp, 16.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color(0xFFFECDD3),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF991B1B),
                        offset = Offset(0.5f, 0.5f),
                        blurRadius = 2f
                    )
                )
            )
        }
    }
}

/**
 * SUPER SAIYAN Nameplate: Radiating Golden Ki Overflow & Electric Energy with Dragon Ball PNG.
 */
@Composable
fun SuperSaiyanNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "saiyan")
    val kiPulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ki"
    )
    val sparkPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparks"
    )

    Box(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFFFACC15).copy(alpha = kiPulse))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF422006), Color(0xFF713F12), Color(0xFFA16207), Color(0xFF422006))
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFFEAB308), Color(0xFFFEF08A), Color(0xFFFACC15))
                ),
                RoundedCornerShape(5.dp)
            )
            .drawBehind {
                // 3 electric spark dots
                for (i in 0..2) {
                    val angle = sparkPhase + (i * 2.094f)
                    val px = size.width * 0.8f + (cos(angle) * 7f)
                    val py = size.height * 0.5f + (sin(angle) * 4f)
                    drawCircle(
                        color = Color(0xFF67E8F9).copy(alpha = (0.5f + 0.5f * sin(angle)).coerceIn(0.2f, 1f)),
                        radius = 1.3f,
                        center = Offset(px, py)
                    )
                }
            }
            .padding(horizontal = 6.5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_super_saiyan),
                contentDescription = "Super Saiyan",
                modifier = Modifier.size(16.dp, 16.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color(0xFFFEF08A),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF854D0E),
                        offset = Offset(0.8f, 0.8f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}

/**
 * LEGENDARY SANNIN Nameplate: Ancient Sage Summon Scroll PNG & Mystic Chakra Aura.
 */
@Composable
fun SanninNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sannin")
    val aura by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sannin_aura"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFF818CF8).copy(alpha = aura))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA), Color(0xFF1E1B4B))
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(listOf(Color(0xFF818CF8), Color(0xFFE0E7FF), Color(0xFF818CF8))),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 6.5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_sannin),
                contentDescription = "Sannin Scroll",
                modifier = Modifier.size(16.dp, 16.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color(0xFFE0E7FF),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/**
 * KAGE Nameplate: Supreme Five Kage Diamond Crest PNG & Obsidian-Ruby Mantle.
 */
@Composable
fun KageNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "kage")
    val rubyGlow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ruby_glow"
    )

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFFE11D48).copy(alpha = rubyGlow))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF4C0519), Color(0xFF881337), Color(0xFFBE123C), Color(0xFF4C0519))
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFF43F5E), Color(0xFFFFD700))),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_kage),
                contentDescription = "Kage Crest",
                modifier = Modifier.size(16.dp, 16.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color(0xFFFFFBEB),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * HOKAGE Nameplate: Blazing Scarlet & Solar Gold Crowned Plate with Fire Hat PNG.
 */
@Composable
fun HokageNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hokage")
    val solarPulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "solar"
    )
    val sweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hokage_sweep"
    )

    Box(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFFFF7B00).copy(alpha = solarPulse))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF450A0A),
                        Color(0xFF7C2D12),
                        Color(0xFFB45309),
                        Color(0xFFEA580C),
                        Color(0xFF450A0A)
                    )
                )
            )
            .border(
                1.4.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFFFFD700), Color(0xFFFFFFFF), Color(0xFFFF7B00))
                ),
                RoundedCornerShape(5.dp)
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val startX = w * sweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0x99FFFFFF), Color(0x88FFD700), Color.Transparent),
                        start = Offset(startX - 15f, 0f),
                        end = Offset(startX + 25f, h)
                    )
                )
            }
            .padding(horizontal = 7.5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_hokage),
                contentDescription = "Hokage Hat",
                modifier = Modifier.size(17.dp, 15.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color(0xFFFFFBEB),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF991B1B),
                        offset = Offset(1f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}

/**
 * AKATSUKI Nameplate: Crimson Cloud Emblem & Midnight Obsidian Robe Plate.
 */
@Composable
fun AkatsukiNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "akatsuki")
    val cloudGlow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "akatsuki_glow"
    )

    Box(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(5.dp), spotColor = Color(0xFFDC2626).copy(alpha = cloudGlow))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0F050A),
                        Color(0xFF2B0A14),
                        Color(0xFF1A050D),
                        Color(0xFF0F050A)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFFFFFFF), Color(0xFFDC2626))),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_akatsuki),
                contentDescription = "Akatsuki Cloud",
                modifier = Modifier.size(16.dp, 16.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color.White,
                fontSize = 8.8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFDC2626),
                        offset = Offset(1f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}

/**
 * ZENKAI GOD Nameplate: Transcendent Cosmic God Crest PNG, Rotating Solar Flares & Iridescent Void.
 */
@Composable
fun ZenkaiGodNameplate(tag: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "zenkai_god")
    val cosmicAura by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cosmic"
    )
    val iridescentSweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iridescent"
    )
    val particlePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFFFFD700).copy(alpha = cosmicAura))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0F0B1E),
                        Color(0xFF2E1065),
                        Color(0xFF581C87),
                        Color(0xFF4C0519),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F0B1E)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFFD700),
                        Color(0xFF38BDF8),
                        Color(0xFFC084FC),
                        Color(0xFFF43F5E),
                        Color(0xFFFFD700)
                    )
                ),
                RoundedCornerShape(6.dp)
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val startX = w * iridescentSweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0x99FFFFFF), Color(0xAAFFD700), Color.Transparent),
                        start = Offset(startX - 20f, 0f),
                        end = Offset(startX + 30f, h)
                    )
                )

                // 4 Floating cosmic stardust embers orbiting the crest
                for (i in 0..3) {
                    val angle = particlePhase + (i * 1.57f)
                    val px = 20f + (cos(angle) * 8f)
                    val py = (h * 0.5f) + (sin(angle) * 5f)
                    drawCircle(
                        color = Color(0xFFFFE066).copy(alpha = (0.4f + 0.6f * sin(angle)).coerceIn(0.2f, 1f)),
                        radius = 1.6f,
                        center = Offset(px, py)
                    )
                }
            }
            .padding(horizontal = 8.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.tag_zenkai_god),
                contentDescription = "Zenkai God Crest",
                modifier = Modifier.size(19.dp, 19.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = tag.uppercase(),
                color = Color(0xFFFFFBEB),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFEAB308),
                        offset = Offset(0f, 0f),
                        blurRadius = 6f
                    )
                )
            )
        }
    }
}

/**
 * DEFAULT / GENERIC Dynamic Tier Nameplate:
 * Clean, modern, high-tier styling scaled by level or rarity.
 */
@Composable
fun GenericTierNameplate(tag: String, level: Int, modifier: Modifier = Modifier) {
    val borderColor = when {
        level >= 100 -> Color(0xFFFFD700)
        level >= 80 -> Color(0xFFC084FC)
        level >= 50 -> Color(0xFFF43F5E)
        level >= 30 -> Color(0xFF38BDF8)
        level >= 15 -> Color(0xFF4ADE80)
        else -> Color(0xFF94A3B8)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, borderColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.uppercase(),
            color = Color.White,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESSIVE ANIMATED LEVEL THEME (Higher levels are significantly more animated)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Progressive Animated Level Theme Badge:
 * Visually distinguishes user power level with escalating animation tiers:
 * - Lv 1-14: Frosted Slate Steel with clean metallic sheen
 * - Lv 15-39: Cyan/Emerald breathing energy pulse with light sweep
 * - Lv 40-79: Blazing Crimson & Gold flame pulse with floating kinetic embers
 * - Lv 80-99: Cosmic Holographic prismatic shimmer with orbiting celestial particles
 * - Lv 100+: Divine Golden & Ultraviolet Hyper-Glow with rotating solar flare rays and celestial embers
 */
@Composable
fun AnimatedLevelThemeBadge(
    level: Int,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "lvl_badge")
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (level >= 100) 800 else if (level >= 40) 1100 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lvl_pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (level >= 80) 1400 else 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lvl_sweep"
    )
    val particles by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lvl_particles"
    )

    when {
        // TIER 5 (Lv 100+): Godly Hyper-Glow & Solar Flare Aura
        level >= 100 -> {
            Box(
                modifier = modifier
                    .shadow(6.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFFFFD700).copy(alpha = pulse))
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF381A03), Color(0xFF78350F), Color(0xFFD97706), Color(0xFF381A03))
                        )
                    )
                    .border(
                        1.4.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFFFFFFF), Color(0xFFF59E0B), Color(0xFFFFD700))
                        ),
                        RoundedCornerShape(6.dp)
                    )
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val startX = w * sweep
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color(0xAAFFFFFF), Color(0x99FFD700), Color.Transparent),
                                start = Offset(startX - 15f, 0f),
                                end = Offset(startX + 25f, h)
                            )
                        )
                        // Floating golden embers
                        for (i in 0..2) {
                            val angle = particles + (i * 2.094f)
                            val px = (w * 0.5f) + (cos(angle) * (w * 0.4f))
                            val py = (h * 0.5f) + (sin(angle) * (h * 0.35f))
                            drawCircle(
                                color = Color(0xFFFFFAF0).copy(alpha = (0.5f + 0.5f * sin(angle)).coerceIn(0.2f, 1f)),
                                radius = 1.5f,
                                center = Offset(px, py)
                            )
                        }
                    }
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lv. $level",
                    color = Color(0xFFFFFBEB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        shadow = Shadow(color = Color(0xFFEAB308), offset = Offset(0f, 0f), blurRadius = 4f)
                    )
                )
            }
        }

        // TIER 4 (Lv 80–99): Cosmic Holographic Prismatic Shimmer
        level >= 80 -> {
            Box(
                modifier = modifier
                    .shadow(4.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFFC084FC).copy(alpha = pulse))
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2E1065), Color(0xFF581C87), Color(0xFF3B0764))
                        )
                    )
                    .border(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFFC084FC), Color(0xFFE9D5FF), Color(0xFFC084FC))
                        ),
                        RoundedCornerShape(6.dp)
                    )
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val startX = w * sweep
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color(0x77FFFFFF), Color.Transparent),
                                start = Offset(startX - 10f, 0f),
                                end = Offset(startX + 20f, h)
                            )
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lv. $level",
                    color = Color(0xFFFAF5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // TIER 3 (Lv 40–79): Blazing Crimson & Scarlet Flame Flare
        level >= 40 -> {
            Box(
                modifier = modifier
                    .shadow(4.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFFE11D48).copy(alpha = pulse))
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF4C0519), Color(0xFF9F1239), Color(0xFF4C0519))
                        )
                    )
                    .border(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFB7185), Color(0xFFFECDD3), Color(0xFFE11D48))
                        ),
                        RoundedCornerShape(6.dp)
                    )
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val startX = w * sweep
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color(0x66FFFFFF), Color.Transparent),
                                start = Offset(startX - 10f, 0f),
                                end = Offset(startX + 15f, h)
                            )
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lv. $level",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // TIER 2 (Lv 15–39): Cobalt & Emerald Breathing Aura
        level >= 15 -> {
            Box(
                modifier = modifier
                    .shadow(3.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFF38BDF8).copy(alpha = pulse))
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0C4A6E), Color(0xFF075985), Color(0xFF0C4A6E))
                        )
                    )
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = pulse), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lv. $level",
                    color = Color(0xFFF0F9FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // TIER 1 (Lv 1–14): Clean Frosted Slate Steel
        else -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x28FFFFFF))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lv. $level",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * High-Quality Level-Scaled & Thematic Tag Badge Router.
 *
 * Rules:
 * - If tag is empty, blank, "null", or "none", returns IMMEDIATELY without rendering anything.
 * - Distinct custom nameplate design for specific tags (Straw Hat, ANBU, Hashira, Sorcerer,
 *   Inferno, Blaze, Spark, Genin, Chunin, Jonin, Demon Slayer, Super Saiyan, Sannin, Kage, Hokage, Zenkai God).
 * - Proper anime PNGs for all title tags with particle and gleam animations.
 * - Higher rarity tags feature richer gradients, animations, and textures.
 * - Completely emoji-free.
 */
@Composable
fun LevelRankTagBadge(
    tag: String?,
    level: Int = 1,
    modifier: Modifier = Modifier
) {
    if (tag.isNullOrBlank() || tag.equals("null", ignoreCase = true) || tag.equals("none", ignoreCase = true)) {
        return
    }

    val clean = tag.trim().replace(Regex("[^\\p{Alnum}\\s_-]"), "").trim()
    val normalized = clean.lowercase()

    when {
        // Anime PNG nameplates
        normalized.contains("straw hat") || normalized == "straw_hat" -> StrawHatNameplate(modifier = modifier)
        normalized == "anbu" -> AnbuNameplate(modifier = modifier)
        normalized.contains("hashira") -> HashiraNameplate(modifier = modifier)
        normalized.contains("sorcerer") || normalized.contains("jujutsu") -> SorcererNameplate(modifier = modifier)
        normalized.contains("demon slayer") -> DemonSlayerNameplate(tag = clean, modifier = modifier)
        normalized.contains("saiyan") -> SuperSaiyanNameplate(tag = clean, modifier = modifier)
        normalized.contains("sannin") -> SanninNameplate(tag = clean, modifier = modifier)
        normalized == "kage" -> KageNameplate(tag = clean, modifier = modifier)
        normalized == "hokage" -> HokageNameplate(tag = clean, modifier = modifier)
        normalized.contains("akatsuki") -> AkatsukiNameplate(tag = clean, modifier = modifier)
        normalized.contains("zenkai god") -> ZenkaiGodNameplate(tag = clean, modifier = modifier)
        normalized == "genin" -> GeninNameplate(tag = clean, modifier = modifier)
        normalized == "chunin" -> ChuninNameplate(tag = clean, modifier = modifier)
        normalized == "jonin" -> JoninNameplate(tag = clean, modifier = modifier)

        // Thematic streak nameplates
        normalized.contains("inferno") -> InfernoNameplate(tag = clean, modifier = modifier)
        normalized.contains("blaze") -> BlazeNameplate(tag = clean, modifier = modifier)
        normalized.contains("spark") -> SparkNameplate(tag = clean, modifier = modifier)
        else -> GenericTierNameplate(tag = clean, level = level, modifier = modifier)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ANIMATED STREAK BADGE (Vibrant Fiery Flame with Breathing Glow)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Hardware-accelerated APNG renderer for the streak flame.
 * Uses APNGDrawable with infinite loop to render the 16-frame convective fire flare.
 * Falls back safely to static painterResource if APNG loading encounters any issue.
 */
@Composable
fun ApngStreakFlame(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var apngDrawable by remember { mutableStateOf<APNGDrawable?>(null) }

    LaunchedEffect(Unit) {
        try {
            val loader = object : StreamLoader() {
                override fun getInputStream(): InputStream =
                    context.resources.openRawResource(R.drawable.ic_streak_flame)
            }
            val drawable = APNGDrawable(loader).apply {
                setLoopLimit(0) // Infinite loop
            }
            apngDrawable = drawable
            drawable.start()
        } catch (_: Throwable) {}
    }

    DisposableEffect(apngDrawable) {
        val d = apngDrawable
        onDispose {
            try { d?.stop() } catch (_: Throwable) {}
        }
    }

    val current = apngDrawable
    if (current != null) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(current)
                    try { current.start() } catch (_: Throwable) {}
                }
            },
            update = { iv ->
                if (iv.drawable !== current) {
                    iv.setImageDrawable(current)
                }
                try { current.start() } catch (_: Throwable) {}
            }
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_streak_flame),
            contentDescription = "Streak Flame",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

/**
 * Animated streak badge with a fiery APNG animated flame graphic and subtle warm glow.
 * Zero emojis. Fixed flame dimensions with NO breathing scaling.
 *
 * @param streakCount  The user's current streak day count.
 * @param isFullLabel  If true, shows "$streakCount Days". If false, shows only "$streakCount".
 */
@Composable
fun AnimatedStreakBadge(
    streakCount: Int,
    modifier: Modifier = Modifier,
    isFullLabel: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "streak_badge")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streak_glow"
    )

    val flameWidth = if (isFullLabel) 14.dp else 10.dp
    val flameHeight = if (isFullLabel) 18.dp else 13.dp
    val fontSize = if (isFullLabel) 11.sp else 8.5.sp
    val hPad = if (isFullLabel) 8.dp else 5.dp
    val vPad = if (isFullLabel) 3.5.dp else 1.5.dp
    val cornerRadius = if (isFullLabel) 20.dp else 6.dp
    val labelText = if (isFullLabel) "$streakCount Days" else "$streakCount"

    Box(
        modifier = modifier
            .shadow(
                elevation = (4 * glowAlpha).dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color(0xFFFF5722).copy(alpha = glowAlpha)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF5A1A08).copy(alpha = 0.92f),
                        Color(0xFF831F05).copy(alpha = 0.95f),
                        Color(0xFF5A1A08).copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFF6D00).copy(alpha = glowAlpha),
                        Color(0xFFFFD600).copy(alpha = glowAlpha * 0.9f),
                        Color(0xFFFF6D00).copy(alpha = glowAlpha)
                    )
                ),
                RoundedCornerShape(cornerRadius)
            )
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.5.dp)
        ) {
            // Upright Flare Burning Flame Animated APNG Asset (Static size, no breathing scale pulse)
            Box(
                modifier = Modifier
                    .width(flameWidth)
                    .height(flameHeight),
                contentAlignment = Alignment.Center
            ) {
                ApngStreakFlame(modifier = Modifier.fillMaxSize())
            }

            Text(
                text = labelText,
                color = Color(0xFFFFE0B2),
                fontSize = fontSize,
                fontWeight = FontWeight.Black
            )
        }
    }
}

