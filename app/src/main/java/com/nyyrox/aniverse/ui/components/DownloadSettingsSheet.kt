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
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer

private val SheetBg = Color(0xFF0F131C)
private val CardBg = Color(0xFF161B26)
private val TextWhite = Color.White
private val TextMuted = Color(0xFF94A3B8)
private val TextSub = Color(0xFF64748B)

/**
 * Download settings sheet styled in clean, sleek monochrome matching the CinePlayer.
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
            DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.INTERNAL)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (glassOn) {
                Box(Modifier.matchParentSize().prismalGlassLayer(glassLayer)) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.radialGradient(
                                listOf(Color(0x18FFFFFF), Color.Transparent),
                                center = Offset(120f, 80f), radius = 900f
                            )
                        )
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.radialGradient(
                                listOf(Color(0x0CFFFFFF), Color.Transparent),
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 24.dp)
                    .navigationBarsPadding()
            ) {
                // Grab handle
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                )
                Spacer(Modifier.height(16.dp))

                // ── Header ──
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(layer, Icons.Default.Tune, TextWhite, size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Download Settings",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "Speed limits, storage location & notifications",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Simultaneous downloads ──
                GlassSection(layer) {
                    SectionTitle(Icons.Default.Speed, TextWhite, "Simultaneous Downloads", "Extra episodes wait in the background queue.")
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5).forEach { count ->
                            val selected = settings.maxConcurrentDownloads == count
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Color.White else Color(0x0EFFFFFF))
                                    .border(1.dp, if (selected) Color.White else Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .clickable { DownloadPreferences.setMaxConcurrentDownloads(context, count) }
                                    .padding(vertical = 11.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$count",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF0F1117) else TextWhite
                                )
                                Text(
                                    if (count == 1) "at a time" else "parallel",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selected) Color(0xFF475569) else TextMuted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when (settings.maxConcurrentDownloads) {
                            1 -> "Best for mobile data or slower connections."
                            2 -> "Balanced: smooth downloads without saturating your network."
                            3 -> "Fast on Wi-Fi when queueing multiple episodes."
                            else -> "Maximum parallel speed for high-speed Wi-Fi."
                        },
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Where files are saved ──
                GlassSection(layer) {
                    SectionTitle(Icons.Default.Folder, TextWhite, "Save Location", "Where finished video files are saved on this device.")
                    Spacer(Modifier.height(14.dp))

                    val isInternal = !customOn && settings.storageLocation == DownloadStorageLocation.INTERNAL
                    val isMovies = !customOn && settings.storageLocation == DownloadStorageLocation.EXTERNAL_PUBLIC

                    LocationOption(
                        layer, Icons.Default.Folder, "App Storage (Private)",
                        "Only playable inside Zenkai. Cleaned on app uninstall.", null, isInternal
                    ) {
                        customOn = false
                        DownloadExporter.setUseCustomFolder(context, false)
                        DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.INTERNAL)
                    }
                    Spacer(Modifier.height(8.dp))
                    LocationOption(
                        layer, Icons.Default.VideoLibrary, "Movies / Zenkai Folder",
                        "Playable in Gallery, VLC, MX Player and external apps.", "Public", isMovies
                    ) {
                        customOn = false
                        DownloadExporter.setUseCustomFolder(context, false)
                        DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.EXTERNAL_PUBLIC)
                    }
                    Spacer(Modifier.height(8.dp))
                    LocationOption(
                        layer,
                        if (customName == null) Icons.Default.CreateNewFolder else Icons.Default.FolderOpen,
                        if (customName == null) "Custom Folder…" else "Folder: $customName",
                        if (customName == null) "Choose any folder or SD card on device." else "Tap to change the destination folder.",
                        null, customOn
                    ) {
                        if (customUri != null && !customOn) {
                            customOn = true
                            DownloadExporter.setUseCustomFolder(context, true)
                            DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.INTERNAL)
                        } else pickFolder()
                    }

                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                    Spacer(Modifier.height(12.dp))

                    ToggleRow(
                        Icons.Default.Help, TextWhite, "Ask Every Time",
                        "Prompt for destination folder before each download starts.",
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
                        TextWhite,
                        "Download Notifications",
                        "Live status bar progress, download speed and completion alerts.",
                        settings.notificationsEnabled
                    ) { DownloadPreferences.setNotificationsEnabled(context, it) }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Pieces ──────────────────────────────────────────────────────────────────────

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

@Composable
private fun Modifier.glassSurface(layer: PrismalGlassLayer?, shape: Shape = RoundedCornerShape(18.dp), radius: Dp = 18.dp, tint: Color? = null): Modifier {
    if (layer == null) {
        return clip(shape).background(tint ?: CardBg).border(1.dp, SolidColor(Color(0x1AFFFFFF)), shape)
    }
    val density = LocalDensity.current
    return drawPrismalGlass(
        backdrop = layer,
        shape = { if (shape == OttPillShape) PrismalCapsule() else PrismalRoundedRectangle(radius) },
        effects = prismalGlassEffects(density),
        onDrawSurface = { drawRect(tint ?: Color(0x0EFFFFFF)) }
    )
}

@Composable
private fun GlassSection(layer: PrismalGlassLayer?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glassSurface(layer, shape = RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun IconBubble(layer: PrismalGlassLayer?, icon: ImageVector, tint: Color, size: Dp = 36.dp) {
    Box(
        Modifier
            .size(size)
            .glassSurface(layer, CircleShape, radius = size / 2, tint = Color(0x14FFFFFF))
            .border(1.dp, Color(0x20FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, accent: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(subtitle, fontSize = 11.5.sp, color = TextMuted, lineHeight = 15.sp)
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
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color(0x20FFFFFF) else Color(0x08FFFFFF))
            .border(1.dp, if (selected) Color(0x60FFFFFF) else Color(0x12FFFFFF), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0x26FFFFFF) else Color(0x0EFFFFFF))
                .border(0.8.dp, if (selected) Color(0x50FFFFFF) else Color(0x14FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) TextWhite else TextMuted, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) TextWhite else TextMuted,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badge != null) {
                    Text(
                        badge,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x26FFFFFF))
                            .border(0.8.dp, Color(0x40FFFFFF), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = if (selected) Color(0xB3FFFFFF) else TextSub, lineHeight = 15.sp)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
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
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(subtitle, fontSize = 11.5.sp, color = TextMuted, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.White.copy(alpha = 0.5f),
                uncheckedThumbColor = Color(0xFFA0A4B2),
                uncheckedTrackColor = Color(0x22FFFFFF),
                uncheckedBorderColor = Color(0x22FFFFFF)
            )
        )
    }
}
