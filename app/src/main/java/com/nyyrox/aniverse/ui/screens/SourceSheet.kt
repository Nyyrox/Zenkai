package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.R

private val SheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val ToggleTrackShape = RoundedCornerShape(10.dp)
private val TogglePillShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSheet(
    servers: List<ServerItem>,
    selectedServerIndex: Int,
    currentAudio: AudioType,
    isDubAvailable: Boolean,
    areAllServersFailed: Boolean,
    onSelectAudio: (AudioType) -> Unit,
    onSelectServer: (Int) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PlayerSheet,
        scrimColor = PlayerScrim,
        shape = SheetShape,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
        ) {
            // SUB / DUB Segmented Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(ToggleTrackShape)
                        .background(PlayerToggleTrack)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SUB Segment
                    val isSubSelected = currentAudio == AudioType.SUB
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(TogglePillShape)
                            .background(if (isSubSelected) PlayerTextActive else PlayerToggleTrack)
                            .clickable(
                                role = Role.RadioButton,
                                onClick = { onSelectAudio(AudioType.SUB) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.player_audio_sub),
                            fontSize = 13.sp,
                            fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSubSelected) PlayerBg else PlayerTextActive,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // DUB Segment
                    val isDubSelected = currentAudio == AudioType.DUB
                    val isDubClickable = isDubAvailable
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(TogglePillShape)
                            .background(if (isDubSelected && isDubAvailable) PlayerTextActive else PlayerToggleTrack)
                            .clickable(
                                enabled = isDubClickable,
                                role = Role.RadioButton,
                                onClick = { onSelectAudio(AudioType.DUB) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.player_audio_dub),
                            fontSize = 13.sp,
                            fontWeight = if (isDubSelected && isDubAvailable) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                !isDubAvailable -> PlayerFaint
                                isDubSelected -> PlayerBg
                                else -> PlayerTextActive
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(thickness = 1.dp, color = PlayerHairline)

            // Server list or Empty State
            if (areAllServersFailed || servers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.player_no_source_available),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PlayerMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.player_action_retry),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayerTextActive,
                        modifier = Modifier
                            .clickable(
                                role = Role.Button,
                                onClick = onRetry
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            } else {
                servers.forEachIndexed { index, server ->
                    val isSelected = index == selectedServerIndex
                    val isClickable = server.isAvailable

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = isClickable,
                                role = Role.Button,
                                onClick = {
                                    onSelectServer(index)
                                    onDismiss()
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Server Name
                        Text(
                            text = server.name,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                !server.isAvailable -> PlayerFaint
                                isSelected -> PlayerTextActive
                                else -> PlayerMuted
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Plain text HD tag
                        if (server.isHd) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.player_quality_hd),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (server.isAvailable) PlayerMuted else PlayerFaint
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        if (!server.isAvailable) {
                            Text(
                                text = stringResource(R.string.player_server_unavailable),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = PlayerFaint
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = PlayerTextActive,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (index < servers.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = PlayerHairline)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
