package com.styropyr0.prismal.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.shapes.PrismalCapsule

internal val LocalPrismalBottomTabScale = staticCompositionLocalOf { { 1f } }

/** Index of the tab that should appear selected (follows the droplet while dragging). */
val LocalPrismalBottomTabHighlightedIndex = staticCompositionLocalOf { { 0 } }

/**
 * Single tab item for use inside [PrismalGlassBottomTabs].
 *
 * Must be placed in the [androidx.compose.foundation.layout.RowScope] [content] lambda
 * of [PrismalGlassBottomTabs].
 */
@Composable
fun RowScope.PrismalGlassBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalPrismalBottomTabScale.current
    Column(
        modifier
            .clip(PrismalCapsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
