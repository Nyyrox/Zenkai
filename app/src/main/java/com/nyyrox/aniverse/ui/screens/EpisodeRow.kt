package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.R
import java.util.Locale

private val ThumbnailShape = RoundedCornerShape(4.dp)
private val SheetTopShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val ActiveRowBrush = Brush.horizontalGradient(
    listOf(PlayerActiveRowGradientStart, Color.Transparent)
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EpisodeRow(
    episode: EpisodeItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onMarkWatched: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsDisabled = isAnimationScaleZero(context)

    var showContextSheet by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rowScale by if (!animationsDisabled) {
        animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1.0f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "row_press_scale"
        )
    } else {
        remember(isPressed) { androidx.compose.runtime.mutableFloatStateOf(if (isPressed) 0.98f else 1.0f) }
    }

    // Colors according to state
    val titleTargetColor = when {
        isActive -> PlayerTextActive
        episode.isWatched -> PlayerFaint
        else -> PlayerMuted
    }
    val titleColor by if (!animationsDisabled) {
        animateColorAsState(
            targetValue = titleTargetColor,
            animationSpec = tween(durationMillis = 120),
            label = "title_color"
        )
    } else {
        remember(titleTargetColor) { mutableStateOf(titleTargetColor) }
    }

    val subLineColor = when {
        isActive -> PlayerTextActive
        episode.isWatched -> PlayerFaint
        else -> PlayerMuted
    }

    val indexColor = when {
        episode.isWatched -> PlayerFaint
        else -> PlayerMuted
    }

    val formattedIndex = remember(episode.n) {
        String.format(Locale.US, "%02d", episode.n)
    }

    val cdSelectedText = stringResource(if (isActive) R.string.player_selected else R.string.player_not_selected)
    val rowContentDesc = stringResource(
        R.string.player_episode_cd_format,
        episode.n,
        episode.title,
        episode.durationText,
        cdSelectedText
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .then(
                if (isActive) Modifier.background(ActiveRowBrush) else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onSelect,
                onLongClick = { showContextSheet = true }
            )
            .clearAndSetSemantics {
                contentDescription = rowContentDesc
                role = Role.Button
            }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [36dp index/equalizer]
            Box(
                modifier = Modifier.size(width = 36.dp, height = 36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isActive) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        Equalizer(
                            isPlaying = isPlaying,
                            color = PlayerTextActive
                        )
                    }
                } else {
                    Text(
                        text = formattedIndex,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = indexColor
                    )
                }
            }

            // [title + sub-line, weight 1f]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = episode.title,
                    fontSize = 16.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (isActive) stringResource(R.string.player_now_playing) else episode.durationText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = subLineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // [96dp 16:9 thumbnail]
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(16f / 9f)
                    .clip(ThumbnailShape)
                    .background(PlayerToggleTrack),
                contentAlignment = Alignment.Center
            ) {
                if (!episode.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .width(96.dp)
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Partially watched: 1dp white progress line under the row, starting at 56dp, width = progress fraction
        if (episode.progressFraction in 0.01f..0.99f && !episode.isWatched) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 56.dp)
                    .fillMaxWidth(fraction = episode.progressFraction.coerceIn(0f, 1f))
                    .height(1.dp)
                    .background(PlayerTextActive)
            )
        }
    }

    // Long-press Context Sheet (Download / Mark watched / Share)
    if (showContextSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showContextSheet = false },
            sheetState = sheetState,
            containerColor = PlayerSheet,
            scrimColor = PlayerScrim,
            shape = SheetTopShape,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                // Header: Episode Title
                Text(
                    text = stringResource(R.string.player_episode_n_title, episode.n, episode.title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlayerTextActive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                HorizontalDivider(thickness = 1.dp, color = PlayerHairline)

                // 1. Download action
                ContextSheetRow(
                    icon = Icons.Default.Download,
                    title = stringResource(R.string.player_action_download),
                    onClick = {
                        showContextSheet = false
                        onDownload()
                    }
                )

                HorizontalDivider(thickness = 1.dp, color = PlayerHairline)

                // 2. Mark watched action
                ContextSheetRow(
                    icon = Icons.Default.Check,
                    title = stringResource(R.string.player_action_mark_watched),
                    onClick = {
                        showContextSheet = false
                        onMarkWatched()
                    }
                )

                HorizontalDivider(thickness = 1.dp, color = PlayerHairline)

                // 3. Share action
                ContextSheetRow(
                    icon = Icons.Default.Share,
                    title = stringResource(R.string.player_action_share),
                    onClick = {
                        showContextSheet = false
                        onShare()
                    }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ContextSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                role = Role.Button
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PlayerTextActive,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = PlayerTextActive
        )
    }
}
