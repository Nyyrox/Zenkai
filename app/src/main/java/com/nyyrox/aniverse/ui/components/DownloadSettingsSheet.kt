package com.nyyrox.aniverse.ui.components

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.data.download.DownloadExporter
import com.nyyrox.aniverse.data.download.DownloadPreferences
import com.nyyrox.aniverse.data.download.DownloadStorageLocation
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer

/**
 * Download settings, same glass rules as DownloadsScreen / the detail page:
 * Settings > Liquid Glassmorphism ON (and API 33+) -> Prismal liquid glass cards over a soft accent wash.
 * OFF -> flat solid AMOLED cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val settings by DownloadPreferences.settings.collectAsState()

    val glassOn = LocalThemeSettings.current.isGlassmorphismEnabled && Build.VERSION.SDK_INT >= 33
    val glassLayer = rememberPrismalGlassLayer()
    val layer = if (glassOn) glassLayer else null

    var askEveryTime by remember { mutableStateOf(DownloadExporter.askEveryTime(context)) }
    var customOn by remember { mutableStateOf(DownloadExporter.useCustomFolder(context)) }
    var customUri by remember { mutableStateOf<Uri?>(DownloadExporter.lastTree(context)) }
    val customName = remember(customUri) { DownloadExporter.folderLabel(context, customUri) }

    val pickFolder = rememberSaveLocationPicker { uri ->
        if (uri != null) {
            customUri = uri
            customOn = true
            DownloadExporter.setUseCustomFolder(context, true)
            // Staging stays in the app; the chosen folder gets the playable copy.
            DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.INTERNAL)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OttBg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (glassOn) {
                // What the glass cards refract.
                Box(Modifier.matchParentSize().prismalGlassLayer(glassLayer)) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.radialGradient(
                                listOf(OttPrimary.copy(alpha = 0.40f), Color.Transparent),
                                center = Offset(120f, 80f), radius = 900f
                            )
                        )
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.radialGradient(
                                listOf(OttSecondary.copy(alpha = 0.28f), Color.Transparent),
                                center = Offset(1000f, 1500f), radius = 1100f
                            )
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 12.dp)
                    .navigationBarsPadding()
            ) {
                // grab handle
                Box(
                    Modifier.align(Alignment.CenterHorizontally).width(38.dp).height(4.dp)
                        .clip(CircleShape).background(Color(0x40FFFFFF))
                )
                Spacer(Modifier.height(14.dp))

                // ── Header ──
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(layer, Icons.Default.Tune, Color.White, size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Download Settings", fontSize = 20.sp, fontWeight = FontWeight.Black, color = OttTextPrimary, letterSpacing = (-0.3).sp)
                        Text("Queue speed & where files are saved", fontSize = 12.sp, color = OttTextSecondary)
                    }
                    Box(
                        Modifier.size(36.dp).glassSurface(layer, CircleShape, radius = 18.dp).clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ── Simultaneous downloads ──
                GlassSection(layer) {
                    SectionTitle(Icons.Default.Speed, OttPrimary, "Simultaneous Downloads", "Extra episodes wait in the queue.")
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5).forEach { count ->
                            val selected = settings.maxConcurrentDownloads == count
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) Color.White else Color(0x14FFFFFF))
                                    .border(1.dp, if (selected) Color.White else Color(0x24FFFFFF), RoundedCornerShape(14.dp))
                                    .clickable { DownloadPreferences.setMaxConcurrentDownloads(context, count) }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$count", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (selected) Color.Black else Color.White)
                                Text(if (count == 1) "at a time" else "parallel", fontSize = 9.sp, fontWeight = FontWeight.Medium,
                                    color = if (selected) Color(0xFF444444) else Color(0x99FFFFFF))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when (settings.maxConcurrentDownloads) {
                            1 -> "Best for mobile data or slow connections."
                            2 -> "Balanced: good speed without saturating your network."
                            3 -> "Great on fast Wi-Fi when queueing a season."
                            else -> "Maximum speed, for fiber connections."
                        },
                        fontSize = 11.sp, color = Color(0xFF9CA3AF)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Where files are saved ──
                GlassSection(layer) {
                    SectionTitle(Icons.Default.Folder, Color(0xFF38BDF8), "Save Location", "Where finished episodes are saved on this device.")
                    Spacer(Modifier.height(12.dp))

                    val isInternal = !customOn && settings.storageLocation == DownloadStorageLocation.INTERNAL
                    val isMovies = !customOn && settings.storageLocation == DownloadStorageLocation.EXTERNAL_PUBLIC

                    LocationOption(
                        layer, Icons.Default.Folder, "App Storage (Private)",
                        "Only playable inside Aniverse. Removed on uninstall.", null, isInternal
                    ) {
                        customOn = false
                        DownloadExporter.setUseCustomFolder(context, false)
                        DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.INTERNAL)
                    }
                    Spacer(Modifier.height(8.dp))
                    LocationOption(
                        layer, Icons.Default.VideoLibrary, "Movies / Zenkai",
                        "Playable in VLC, MX Player, Files and Gallery.", "VLC / Gallery", isMovies
                    ) {
                        customOn = false
                        DownloadExporter.setUseCustomFolder(context, false)
                        DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.EXTERNAL_PUBLIC)
                    }
                    Spacer(Modifier.height(8.dp))
                    LocationOption(
                        layer,
                        if (customName == null) Icons.Default.CreateNewFolder else Icons.Default.FolderOpen,
                        if (customName == null) "Choose a folder…" else "Folder: $customName",
                        if (customName == null) "Pick any folder, SD card included." else "Tap to change the folder.",
                        null, customOn
                    ) {
                        if (customUri != null && !customOn) {
                            customOn = true
                            DownloadExporter.setUseCustomFolder(context, true)
                            DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.INTERNAL)
                        } else pickFolder()
                    }

                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x18FFFFFF)))
                    Spacer(Modifier.height(12.dp))

                    ToggleRow(
                        Icons.Default.Help, Color(0xFFA78BFA), "Ask every time",
                        "Pick the folder each time you download to your device.",
                        askEveryTime
                    ) {
                        askEveryTime = it
                        DownloadExporter.setAskEveryTime(context, it)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Notifications ──
                GlassSection(layer) {
                    ToggleRow(
                        Icons.Default.Notifications,
                        if (settings.notificationsEnabled) Color(0xFFFBBF24) else OttTextSecondary,
                        "Download Notifications",
                        "Live progress, speed and completion alerts.",
                        settings.notificationsEnabled
                    ) { DownloadPreferences.setNotificationsEnabled(context, it) }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Pieces ──────────────────────────────────────────────────────────────────────

/** Opens the system folder picker; returns a function that launches it. onPicked(null) = cancelled. */
@Composable
private fun rememberSaveLocationPicker(onPicked: (Uri?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) DownloadExporter.persistTree(context, uri)
        latest.value(uri)
    }
    return remember(launcher) { { launcher.launch(DownloadExporter.lastTree(context)) } }
}

/** Glass on: Prismal liquid glass over the sheet's backdrop layer. Glass off: solid AMOLED + hairline. */
@Composable
private fun Modifier.glassSurface(layer: PrismalGlassLayer?, shape: Shape = OttCardShape, radius: Dp = 22.dp, tint: Color? = null): Modifier {
    if (layer == null) {
        return clip(shape).background(tint ?: Color(0xFF0D1017)).border(1.dp, SolidColor(Color(0x1AFFFFFF)), shape)
    }
    val density = LocalDensity.current
    return drawPrismalGlass(
        backdrop = layer,
        shape = { if (shape == OttPillShape) PrismalCapsule() else PrismalRoundedRectangle(radius) },
        effects = prismalGlassEffects(density),
        onDrawSurface = { drawRect(tint ?: Color(0x0FFFFFFF)) }
    )
}

@Composable
private fun GlassSection(layer: PrismalGlassLayer?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().glassSurface(layer).padding(16.dp), content = content)
}

@Composable
private fun IconBubble(layer: PrismalGlassLayer?, icon: ImageVector, tint: Color, size: Dp = 36.dp) {
    Box(Modifier.size(size).glassSurface(layer, CircleShape, radius = size / 2), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, accent: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 11.5.sp, color = OttTextSecondary, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun LocationOption(
    layer: PrismalGlassLayer?,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color(0x26FFFFFF) else Color(0x0AFFFFFF))
            .border(1.dp, if (selected) Color(0x70FFFFFF) else Color(0x1AFFFFFF), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(layer, icon, if (selected) Color.White else OttTextSecondary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else OttTextSecondary, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                if (badge != null) {
                    Text(
                        badge, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399),
                        modifier = Modifier.clip(OttPillShape).background(Color(0x2A10B981)).border(0.8.dp, Color(0x6010B981), OttPillShape).padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Text(subtitle, fontSize = 10.5.sp, color = Color(0x99FFFFFF), lineHeight = 14.sp)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = OttTextSecondary, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OttPrimary,
                uncheckedThumbColor = Color(0xFFA0A4B2),
                uncheckedTrackColor = Color(0x30FFFFFF),
                uncheckedBorderColor = Color(0x30FFFFFF)
            )
        )
    }
}
