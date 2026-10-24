package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.nyyrox.aniverse.ui.theme.OttBloodPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.util.AppHaptics

/**
 * Unified 40dp glass icon button — the ONE component used by TopBar, DetailScreen
 * back/groups/bookmark buttons — so they all look identical.
 *
 * - 40dp circle
 * - 12% white overlay bg (0x1EFFFFFF)
 * - 1dp border at 20% white opacity (0x33FFFFFF) — the glass border is what makes it read as glass
 * - Icon 20dp
 * - Press state: scale 0.92, opacity 0.80, 100ms FastOutLinearIn
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tintColor: Color = Color.White,
    backgroundTint: Color = Color(0x1EFFFFFF),
    borderColor: Color = Color(0x33FFFFFF)
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100, easing = FastOutLinearInEasing),
        label = "glass_btn_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1f,
        animationSpec = tween(100, easing = FastOutLinearInEasing),
        label = "glass_btn_alpha"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(backgroundTint)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    AppHaptics.tick(context)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Unified metadata chip used in DetailScreen hero section and card badges.
 * - Fixed height 28dp
 * - Corner radius 8dp
 * - Consistent text size 13sp
 * - Optional leading icon (for rating star)
 * - Optional accent color for icon (defaults to null = no icon)
 */
@Composable
fun MetadataChip(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = Color.White,
    textColor: Color = Color.White,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val base = modifier
        .height(28.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
            Brush.verticalGradient(
                listOf(Color(0x30FFFFFF), Color(0x12FFFFFF))
            )
        )
        .border(
            1.dp,
            Brush.verticalGradient(
                listOf(Color(0x55FFFFFF), Color(0x18FFFFFF))
            ),
            RoundedCornerShape(8.dp)
        )
        .then(
            if (onClick != null) Modifier.clickable {
                AppHaptics.tick(context)
                onClick()
            } else Modifier
        )

    Row(
        modifier = base.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            softWrap = false,
            maxLines = 1
        )
    }
}

/**
 * Unified pill toggle row — used for DIRECT/EMBED and SUB/DUB/MULTI-AUDIO selectors
 * so both rows share one visual identity.
 */
@Composable
fun OttPillToggle(
    options: List<Pair<String, String>>, // key to display label
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .clip(OttPillShape)
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), OttPillShape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (key, label) ->
            val isSelected = selected == key
            Box(
                modifier = Modifier
                    .clip(OttPillShape)
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable {
                        if (selected != key) {
                            AppHaptics.tick(context)
                            onSelect(key)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                    softWrap = false,
                    maxLines = 1
                )
            }
        }
    }
}

// Re-export OttPillShape so callers can use it inside this file
private val OttPillShape = com.nyyrox.aniverse.ui.theme.OttPillShape
