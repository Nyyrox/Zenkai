package com.nyyrox.aniverse.player
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.graphics.Bitmap
import android.os.Build
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.LocalPrismalBackdrop
import android.view.LayoutInflater
import android.os.Message
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import androidx.annotation.OptIn
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.nyyrox.aniverse.data.SkipInterval
import com.nyyrox.aniverse.data.SubtitleCue
import com.nyyrox.aniverse.data.aniskip.AniSkipRepository
import com.nyyrox.aniverse.data.subtitles.SubtitleParser
import com.nyyrox.aniverse.data.subtitles.TranslationService
import com.nyyrox.aniverse.data.subtitles.findCueAt
import com.nyyrox.aniverse.data.subtitles.findCueIndexAt
import com.nyyrox.aniverse.data.subtitles.findActiveCuesAt
import kotlinx.coroutines.Dispatchers
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nyyrox.aniverse.MainActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import com.nyyrox.aniverse.data.extractor.StreamServerOption
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.subtitles.SubtitleRepository
import com.nyyrox.aniverse.data.subtitles.SubtitleTrack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.CompositionLocalProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nyyrox.aniverse.service.PlayerNotificationService
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.util.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

enum class PlayerEngineMode {
    SNIFFING,
    NATIVE_EXO,
    IFRAME_FALLBACK
}

private fun formatAudioTrackLabel(format: androidx.media3.common.Format, index: Int): String {
    val rawLabel = format.label?.trim()
    val rawLang = (format.language ?: "").trim().lowercase(Locale.US)
    val rawId = (format.id ?: "").trim().lowercase(Locale.US)

    val isGenericLabel = rawLabel.isNullOrBlank() ||
            rawLabel.equals("audio", ignoreCase = true) ||
            rawLabel.equals("main", ignoreCase = true) ||
            rawLabel.equals("default", ignoreCase = true) ||
            rawLabel.matches(Regex("^(audio|track|stream|sound)[_ -]*\\d*$", RegexOption.IGNORE_CASE))

    val langCode = when {
        rawLang.isNotBlank() && rawLang != "und" -> rawLang
        rawId.contains("jpn") || rawId.contains("ja") || (!rawLabel.isNullOrBlank() && rawLabel.contains("jap", ignoreCase = true)) -> "ja"
        rawId.contains("eng") || rawId.contains("en") || (!rawLabel.isNullOrBlank() && rawLabel.contains("eng", ignoreCase = true)) -> "en"
        rawId.contains("hin") || rawId.contains("hi") || (!rawLabel.isNullOrBlank() && rawLabel.contains("hin", ignoreCase = true)) -> "hi"
        rawId.contains("tam") || rawId.contains("ta") || (!rawLabel.isNullOrBlank() && rawLabel.contains("tam", ignoreCase = true)) -> "ta"
        rawId.contains("tel") || rawId.contains("te") || (!rawLabel.isNullOrBlank() && rawLabel.contains("tel", ignoreCase = true)) -> "te"
        rawId.contains("ben") || rawId.contains("bn") || (!rawLabel.isNullOrBlank() && rawLabel.contains("ben", ignoreCase = true)) -> "bn"
        rawId.contains("mal") || rawId.contains("ml") || (!rawLabel.isNullOrBlank() && rawLabel.contains("mal", ignoreCase = true)) -> "ml"
        rawId.contains("kan") || rawId.contains("kn") || (!rawLabel.isNullOrBlank() && rawLabel.contains("kan", ignoreCase = true)) -> "kn"
        rawId.contains("mar") || rawId.contains("mr") || (!rawLabel.isNullOrBlank() && rawLabel.contains("mar", ignoreCase = true)) -> "mr"
        rawId.contains("kor") || rawId.contains("ko") || (!rawLabel.isNullOrBlank() && rawLabel.contains("kor", ignoreCase = true)) -> "ko"
        rawId.contains("zho") || rawId.contains("chi") || rawId.contains("zh") || (!rawLabel.isNullOrBlank() && rawLabel.contains("chi", ignoreCase = true)) -> "zh"
        rawId.contains("spa") || rawId.contains("es") || (!rawLabel.isNullOrBlank() && rawLabel.contains("spa", ignoreCase = true)) -> "es"
        rawId.contains("fre") || rawId.contains("fra") || rawId.contains("fr") || (!rawLabel.isNullOrBlank() && rawLabel.contains("fre", ignoreCase = true)) -> "fr"
        rawId.contains("ger") || rawId.contains("deu") || rawId.contains("de") || (!rawLabel.isNullOrBlank() && rawLabel.contains("ger", ignoreCase = true)) -> "de"
        rawId.contains("ita") || rawId.contains("it") || (!rawLabel.isNullOrBlank() && rawLabel.contains("ita", ignoreCase = true)) -> "it"
        rawId.contains("por") || rawId.contains("pt") || (!rawLabel.isNullOrBlank() && rawLabel.contains("por", ignoreCase = true)) -> "pt"
        rawId.contains("rus") || rawId.contains("ru") || (!rawLabel.isNullOrBlank() && rawLabel.contains("rus", ignoreCase = true)) -> "ru"
        rawId.contains("ara") || rawId.contains("ar") || (!rawLabel.isNullOrBlank() && rawLabel.contains("ara", ignoreCase = true)) -> "ar"
        rawId.contains("ind") || rawId.contains("id") || (!rawLabel.isNullOrBlank() && rawLabel.contains("ind", ignoreCase = true)) -> "id"
        rawId.contains("tha") || rawId.contains("th") || (!rawLabel.isNullOrBlank() && rawLabel.contains("tha", ignoreCase = true)) -> "th"
        rawId.contains("vie") || rawId.contains("vi") || (!rawLabel.isNullOrBlank() && rawLabel.contains("vie", ignoreCase = true)) -> "vi"
        else -> ""
    }

    val languageName = when (langCode) {
        "ja", "jpn" -> "Japanese (Original)"
        "en", "eng" -> "English (Dub)"
        "hi", "hin" -> "Hindi (Dub)"
        "ta", "tam" -> "Tamil (Dub)"
        "te", "tel" -> "Telugu (Dub)"
        "bn", "ben" -> "Bengali (Dub)"
        "ml", "mal" -> "Malayalam (Dub)"
        "kn", "kan" -> "Kannada (Dub)"
        "mr", "mar" -> "Marathi (Dub)"
        "ko", "kor" -> "Korean"
        "zh", "zho", "chi" -> "Chinese"
        "es", "spa" -> "Spanish"
        "fr", "fra", "fre" -> "French"
        "de", "deu", "ger" -> "German"
        "it", "ita" -> "Italian"
        "pt", "por" -> "Portuguese"
        "ru", "rus" -> "Russian"
        "ar", "ara" -> "Arabic"
        "id", "ind" -> "Indonesian"
        "th", "tha" -> "Thai"
        "vi", "vie" -> "Vietnamese"
        else -> try {
            if (langCode.isNotBlank()) Locale(langCode).getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.uppercase() } else ""
        } catch (_: Exception) { "" }
    }

    val baseTitle = when {
        !isGenericLabel -> rawLabel!!
        languageName.isNotBlank() -> languageName
        else -> "Audio Track ${index + 1}"
    }

    val channelInfo = when (format.channelCount) {
        1 -> " (Mono)"
        2 -> " (Stereo)"
        6 -> " (5.1)"
        8 -> " (7.1)"
        else -> ""
    }

    return if (!isGenericLabel && languageName.isNotBlank() && !rawLabel!!.contains(languageName, ignoreCase = true)) {
        "$rawLabel ($languageName)$channelInfo"
    } else {
        "$baseTitle$channelInfo"
    }
}

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

data class QualityTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val height: Int,
    val bitrate: Int,
    val label: String,
    val isSelected: Boolean
)

data class SubtitleTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

data class CaptionCustomization(
    val fontSizeSp: Float = 18f,
    val textColor: Color = Color.White,
    val bgAlpha: Float = 0.55f,
    val textAlpha: Float = 1.0f,
    val hasShadow: Boolean = true,
    val isBold: Boolean = true,
    val hasStroke: Boolean = true,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 3.0f
)

enum class CaptionPlayStatus {
    OFF,
    DOWNLOADING,
    PLAYING,
    ERROR
}

@Composable
private fun CinePillIndicator(
    fraction: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val f = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
    GlassPill(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x40FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(f)
                        .background(Color.White)
                )
            }
            Text(
                text = "${(f * 100).toInt()}%",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.width(34.dp)
            )
        }
    }
}

@Composable
private fun DoubleTapSeekOverlay(
    isForward: Boolean,
    accumulatedSeconds: Int,
    triggerKey: Int,
    transitionStyle: SeekTransitionStyle = SeekTransitionStyle.CHEVRON_SWEEP,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "seekTransition")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val scaleAnim = remember { Animatable(1.18f) }
    LaunchedEffect(triggerKey) {
        scaleAnim.snapTo(1.22f)
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.42f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.scale(scaleAnim.value)
        ) {
            // Animated Chevrons with smooth sine wave pulse
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    val idx = if (isForward) i else (2 - i)
                    val waveOffset = idx * 0.28f
                    val waveNorm = (animPhase + waveOffset) % 1f
                    val alpha = 0.35f + 0.65f * kotlin.math.sin(waveNorm * Math.PI.toFloat()).coerceAtLeast(0f)

                    Icon(
                        imageVector = if (isForward) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = alpha),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Text(
                text = "${if (isForward) "+" else "-"}${accumulatedSeconds}s",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = if (isForward) "FORWARD" else "REWIND",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

private enum class PlayerSettingsTab {
    MAIN,
    VIDEO,
    PICTURE,
    PLAYBACK,
    QUALITY,
    SERVER,
    ORIENTATION,
    SPEED,
    AUDIO,
    THEME_PACKS,
    GESTURES
}

// ── Web CinePlayer Design Tokens & CSS Components ───────────────────────────
private val ZpRed = Color(0xFFDC143C)
private val ZpRed2 = Color(0xFFFF4D6A)

private data class Anime4kModeInfo(val id: String, val label: String, val desc: String)

private val ANIME4K_MODES = listOf(
    Anime4kModeInfo("modeA", "Mode A", "Balanced restoration & 2× upscale (Best for 720p/1080p anime)"),
    Anime4kModeInfo("modeB", "Mode B", "Soft restoration (Reduces compression ringing & artifacts)"),
    Anime4kModeInfo("modeC", "Mode C", "Sharp outlines & high detail for crisp modern animation"),
    Anime4kModeInfo("modeAA", "Mode AA", "Double-pass ultra restoration for maximum clean detail"),
    Anime4kModeInfo("denoise", "Denoise", "Artifact removal & debanding + 2× CNN upscale"),
    Anime4kModeInfo("fast", "Fast CNN", "Lightweight 2× neural net (low battery / integrated GPU)")
)

private data class FilterPresetInfo(val id: String, val label: String)

private val VIDEO_FILTER_PRESETS = listOf(
    FilterPresetInfo("off", "Default"),
    FilterPresetInfo("vibrant", "Vibrance"),
    FilterPresetInfo("cinema", "Cinema HDR"),
    FilterPresetInfo("sharp", "Crisp Lines"),
    FilterPresetInfo("warm", "Warm (Eye Care)")
)

private val VIDEO_ASPECTS = listOf("Fit", "Fill", "Stretch", "16:9", "4:3")

private fun computeVideoColorMatrix(
    preset: String,
    brightnessPct: Int,
    contrastPct: Int,
    saturationPct: Int,
    upscalerEnabled: Boolean,
    upscalerIntensity: Int,
    upscalerMode: String
): ColorMatrix {
    var b = (brightnessPct - 100) / 100f
    var c = contrastPct / 100f
    var s = saturationPct / 100f

    when (preset) {
        "vibrant" -> {
            s *= 1.35f
            c *= 1.08f
        }
        "cinema" -> {
            c *= 1.22f
            s *= 1.12f
            b -= 0.03f
        }
        "sharp" -> {
            c *= 1.28f
            s *= 1.05f
        }
        "warm" -> {
            s *= 1.05f
        }
    }

    if (upscalerEnabled) {
        val intensity = (upscalerIntensity / 100f).coerceIn(0f, 1f)
        when (upscalerMode) {
            "modeA" -> {
                c *= (1f + 0.18f * intensity)
                s *= (1f + 0.10f * intensity)
            }
            "modeB" -> {
                c *= (1f + 0.08f * intensity)
                b += (0.02f * intensity)
            }
            "modeC" -> {
                c *= (1f + 0.25f * intensity)
                s *= (1f + 0.05f * intensity)
            }
            "modeAA" -> {
                c *= (1f + 0.30f * intensity)
                s *= (1f + 0.15f * intensity)
            }
            "denoise" -> {
                c *= (1f + 0.05f * intensity)
                b += (0.01f * intensity)
            }
            "fast" -> {
                c *= (1f + 0.12f * intensity)
                s *= (1f + 0.08f * intensity)
            }
        }
    }

    val lr = 0.2126f
    val lg = 0.7152f
    val lb = 0.0722f

    val invSat = 1f - s
    val rR = invSat * lr + s
    val rG = invSat * lg
    val rB = invSat * lb

    val gR = invSat * lr
    val gG = invSat * lg + s
    val gB = invSat * lb

    val bR = invSat * lr
    val bG = invSat * lg
    val bB = invSat * lb + s

    val contrastOffset = 0.5f * (1f - c) * 255f
    val brightnessOffset = b * 255f
    val totalOffset = contrastOffset + brightnessOffset

    val warmR = if (preset == "warm") 1.08f else 1.0f
    val warmG = if (preset == "warm") 1.02f else 1.0f
    val warmB = if (preset == "warm") 0.90f else 1.0f

    val values = floatArrayOf(
        rR * c * warmR, rG * c, rB * c, 0f, totalOffset,
        gR * c, gG * c * warmG, gB * c, 0f, totalOffset,
        bR * c, bG * c, bB * c * warmB, 0f, totalOffset,
        0f, 0f, 0f, 1f, 0f
    )
    return ColorMatrix(values)
}

/** Web Frosted Button (.zp-b in cineplayer.css) */
@Composable
private fun ZpBtn(
    size: androidx.compose.ui.unit.Dp = 36.dp,
    modifier: Modifier = Modifier,
    isPill: Boolean = false,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    onClick: (Rect) -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val src = remember { MutableInteractionSource() }
    val isPressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.64f, stiffness = Spring.StiffnessMedium),
        label = "zpBtnScale"
    )
    val bg = if (isSelected) ZpRed.copy(alpha = 0.40f) else Color(0x1FFFFFFF)
    val borderCol = if (isSelected) ZpRed else Color(0x29FFFFFF)
    val shape = if (isPill) RoundedCornerShape(99.dp) else CircleShape

    Row(
        modifier = modifier
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .scale(scale)
            .alpha(if (enabled) 1f else 0.32f)
            .then(if (isPill) Modifier.height(34.dp) else Modifier.size(size))
            .clip(shape)
            .background(bg)
            .border(1.dp, borderCol, shape)
            .clickable(
                interactionSource = src,
                indication = null,
                enabled = enabled,
                onClick = { onClick(bounds) }
            )
            .then(if (isPill) Modifier.padding(horizontal = 12.dp) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

/** Web Hero Play/Pause Button (.zp-hero in cineplayer.css) */
@Composable
private fun ZpHeroBtn(
    isPlaying: Boolean,
    size: androidx.compose.ui.unit.Dp = 68.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val isPressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.64f, stiffness = Spring.StiffnessMedium),
        label = "heroScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .border(2.dp, ZpRed2.copy(alpha = pulseAlpha), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ZpRed, ZpRed2)))
                .clickable(
                    interactionSource = src,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(140)) + scaleIn(initialScale = 0.75f, animationSpec = tween(140)))
                        .togetherWith(fadeOut(tween(100)) + scaleOut(targetScale = 0.75f, animationSpec = tween(100)))
                },
                label = "heroPlayPauseMorph"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.46f)
                )
            }
        }
    }
}

/** Web Seek Button (.zp-sd in cineplayer.css) */
@Composable
private fun ZpSeekBtn(
    isForward: Boolean,
    seconds: Int = 10,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    onClick: () -> Unit
) {
    ZpBtn(size = size, onClick = { onClick() }) {
        Icon(
            imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
            contentDescription = if (isForward) "+${seconds}s" else "-${seconds}s",
            tint = Color.White,
            modifier = Modifier.size(size * 0.62f)
        )
    }
}

/** Web Toggle Switch (.zp-tg in cineplayer.css) */
@Composable
private fun ZpToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "toggleThumb"
    )
    val trackBg by animateColorAsState(
        targetValue = if (checked) ZpRed else Color(0x33FFFFFF),
        animationSpec = tween(160),
        label = "toggleBg"
    )

    Box(
        modifier = modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(trackBg)
            .clickable { onCheckedChange(!checked) }
            .padding(2.5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(19.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** Web Selectable Pill (.zp-pi in cineplayer.css) */
@Composable
private fun ZpPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) ZpRed else Color(0x18FFFFFF)
    val borderCol = if (isSelected) Color.Transparent else Color(0x22FFFFFF)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = Color.White
        )
    }
}

/** Web Section Header (.zp-sec in cineplayer.css) */
@Composable
private fun ZpSec(title: String) {
    Text(
        text = title,
        color = Color(0xFFDDDDDD),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

/** Web Explanatory Note (.zp-note in cineplayer.css) */
@Composable
private fun ZpNote(text: String) {
    Text(
        text = text,
        color = Color(0x99FFFFFF),
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

/** Web Setting Row with Toggle (.zp-row in cineplayer.css) */
@Composable
private fun ZpRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium
        )
        ZpToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Web AniSkip floating button (.zp-sk in cineplayer.css) */
@Composable
private fun ZpAniSkipBtn(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xD9000000))
            .border(1.dp, ZpRed, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.FastForward,
            contentDescription = null,
            tint = ZpRed,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x22FFFFFF))
                .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = "Tab",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Web Episode Welcome Card (.zp-card in cineplayer.css) */
@Composable
private fun ZpEpisodeCard(
    title: String,
    episode: Int,
    episodeTitle: String?,
    isDub: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(start = 20.dp, bottom = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xB3000000))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ZpRed)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Episode $episode" + (if (!episodeTitle.isNullOrBlank()) " · $episodeTitle" else "") + (if (isDub) " · DUB" else " · SUB"),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@OptIn(UnstableApi::class)
@Composable
fun CinePlayerView(
    title: String,
    episode: Int,
    episodeTitle: String? = null,
    nextEpisodeTitle: String? = null,
    nextEpisodeImage: String? = null,
    streamUrl: String,
    isDirectStream: Boolean,
    isDub: Boolean = false,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onBack: () -> Unit = {},
    onCollapseToMiniPlayer: (() -> Unit)? = null,
    onDragDownDelta: ((Float) -> Unit)? = null,
    onDragDownRelease: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialTimeMs: Long = 0L,
    startPaused: Boolean = false,
    externalSubtitles: List<SubtitleTrack> = emptyList(),
    headers: Map<String, String> = emptyMap(),
    malId: Int? = null,
    anilistId: Int = 0,
    season: Int = 1,
    maxEpisode: Int = 1,
    onSelectEpisode: ((Int) -> Unit)? = null,
    hasPreviousEpisode: Boolean = false,
    onPreviousEpisode: () -> Unit = {},
    hasNextEpisode: Boolean = false,
    onNextEpisode: () -> Unit = {},
    isAutoPlayNext: Boolean = true,
    onToggleAutoPlayNext: (Boolean) -> Unit = {},
    isAutoSkipIntroOutro: Boolean = false,
    onToggleAutoSkipIntroOutro: (Boolean) -> Unit = {},
    onProgressUpdate: (currentMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onPlaybackFailed: () -> Unit = {},
    availableServers: List<StreamServerOption> = emptyList(),
    onSelectServer: ((StreamServerOption) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageManager = remember { com.nyyrox.aniverse.data.StorageManager(context) }
    var playerCustomization by remember { mutableStateOf(storageManager.getCinePlayerCustomization()) }
    val currentGestureSettings by rememberUpdatedState(playerCustomization.gestures)
    val themePalette = remember(playerCustomization.themeId) { PlayerThemePalette.fromId(playerCustomization.themeId) }

    // Lock position normalized fractions
    var lockXFraction by remember(playerCustomization.placement) {
        mutableFloatStateOf(
            when (playerCustomization.placement.lockAnchor) {
                LockScreenAnchor.TOP_LEFT -> 0.03f
                LockScreenAnchor.TOP_RIGHT -> 0.92f
                LockScreenAnchor.BOTTOM_LEFT -> 0.03f
                LockScreenAnchor.BOTTOM_RIGHT -> 0.92f
                LockScreenAnchor.TOP_CENTER -> 0.48f
                LockScreenAnchor.CUSTOM_FREEHAND -> playerCustomization.placement.lockCustomXFraction
            }
        )
    }
    var lockYFraction by remember(playerCustomization.placement) {
        mutableFloatStateOf(
            when (playerCustomization.placement.lockAnchor) {
                LockScreenAnchor.TOP_LEFT -> 0.05f
                LockScreenAnchor.TOP_RIGHT -> 0.05f
                LockScreenAnchor.BOTTOM_LEFT -> 0.88f
                LockScreenAnchor.BOTTOM_RIGHT -> 0.88f
                LockScreenAnchor.TOP_CENTER -> 0.05f
                LockScreenAnchor.CUSTOM_FREEHAND -> playerCustomization.placement.lockCustomYFraction
            }
        )
    }

    val initialAutoPlay = remember(isAutoPlayNext) {
        if (isAutoPlayNext) true else storageManager.isAutoPlayNext()
    }
    var autoPlayNextEnabled by remember(initialAutoPlay) { mutableStateOf(initialAutoPlay) }
    // the player listener lives in a long-running effect; plain params would be stale there
    val hasNextNow by rememberUpdatedState(hasNextEpisode)
    val onNextNow by rememberUpdatedState(onNextEpisode)
    val initialAutoSkip = remember(isAutoSkipIntroOutro) {
        if (isAutoSkipIntroOutro) true else storageManager.isAutoSkipIntroOutro()
    }
    var autoSkipIntroOutroEnabled by remember(initialAutoSkip) { mutableStateOf(initialAutoSkip) }
    var isNextTriggered by remember(episode, streamUrl) { mutableStateOf(false) }
    var isUpNextCanceled by remember(episode, streamUrl) { mutableStateOf(false) }
    var showPlayerSettingsModal by remember { mutableStateOf(false) }
    var activeSettingsTab by remember { mutableStateOf(PlayerSettingsTab.MAIN) }

    // Web CinePlayer State: Video, Picture, Playback & Shaders
    var currentIsDub by remember(isDub) { mutableStateOf(isDub) }
    var videoAspectRatio by remember { mutableStateOf("Fit") }
    var videoZoomFactor by remember { mutableFloatStateOf(100f) }
    var isMirrored by remember { mutableStateOf(false) }
    var isUpscalerEnabled by remember { mutableStateOf(false) }
    var upscalerMode by remember { mutableStateOf("modeA") }
    var upscalerIntensity by remember { mutableIntStateOf(80) }
    var videoFilterPreset by remember { mutableStateOf("off") }
    var videoBrightness by remember { mutableIntStateOf(100) }
    var videoContrast by remember { mutableIntStateOf(100) }
    var videoSaturation by remember { mutableIntStateOf(100) }
    var skipStepSeconds by remember { mutableIntStateOf(10) }
    var holdSpeedMultiplier by remember { mutableFloatStateOf(2.0f) }
    var autoHideTimeoutMs by remember { mutableLongStateOf(3200L) }
    var skipFillerEnabled by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var showEpisodeWelcomeCard by remember { mutableStateOf(true) }

    LaunchedEffect(episode, streamUrl) {
        showEpisodeWelcomeCard = true
        kotlinx.coroutines.delay(4000L)
        showEpisodeWelcomeCard = false
    }

    val videoColorMatrix = remember(
        videoFilterPreset,
        videoBrightness,
        videoContrast,
        videoSaturation,
        isUpscalerEnabled,
        upscalerIntensity,
        upscalerMode
    ) {
        computeVideoColorMatrix(
            preset = videoFilterPreset,
            brightnessPct = videoBrightness,
            contrastPct = videoContrast,
            saturationPct = videoSaturation,
            upscalerEnabled = isUpscalerEnabled,
            upscalerIntensity = upscalerIntensity,
            upscalerMode = upscalerMode
        )
    }

    val currentResizeMode = when (videoAspectRatio) {
        "Fill" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        "Stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        "16:9" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        "4:3" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    // Mini player handoff: start already in NATIVE_EXO with the parked stream so no loader flashes
    val handoff = remember(streamUrl) { MiniPlayerManager.peek(anilistId, episode, streamUrl) }
    var engineMode by remember(streamUrl) {
        mutableStateOf(
            if (isDirectStream || handoff != null) PlayerEngineMode.NATIVE_EXO else PlayerEngineMode.SNIFFING
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            // mini-player handoff: DisposableEffect(streamUrl) below parks or detaches
            if (MiniPlayerManager.freshRequest() != null) return@onDispose
            com.nyyrox.aniverse.service.ActivePlayerHolder.detach()
            val stopIntent = Intent(context, com.nyyrox.aniverse.service.PlayerNotificationService::class.java).apply {
                action = PlayerNotificationService.ACTION_STOP
            }
            try {
                context.startService(stopIntent)
            } catch (_: Exception) {}
        }
    }

    var sniffedStream by remember(streamUrl) { mutableStateOf<SniffedStream?>(handoff?.sniffed) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(handoff?.player) }
    val listenerRefs = remember { mutableListOf<Player.Listener>() }

    // Playback States
    var isPlaying by remember { mutableStateOf(handoff?.player?.playWhenReady ?: true) }
    val isInPip by remember { MainActivity.isInPipMode }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                val act = context as? ComponentActivity
                val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    act?.isInPictureInPictureMode == true
                } else false
                if (!inPip) {
                    exoPlayer?.pause()
                    isPlaying = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    var isBuffering by remember { mutableStateOf(handoff == null) }
    var hasStartedPlayback by remember { mutableStateOf(handoff != null) }   // true after the first frame is ready
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var showTimeRemaining by remember { mutableStateOf(false) }
    var hasAppliedInitialSeek by remember(episode, streamUrl) { mutableStateOf(false) }
    var resumeToastText by remember(episode, streamUrl) { mutableStateOf<String?>(null) }
    var playbackErrorCode by remember(streamUrl) { mutableStateOf<String?>(null) }
    var playbackErrorInfo by remember(streamUrl) { mutableStateOf<String?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var fitNotificationText by remember { mutableStateOf<String?>(null) }

    fun notifyFitChange(newMode: Int) {
        resizeMode = newMode
        val label = when (newMode) {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Fit: Crop / Zoom"
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fit: Stretch"
            else -> "Fit: Fit to Screen"
        }
        fitNotificationText = label
        scope.launch {
            delay(2000)
            if (fitNotificationText == label) {
                fitNotificationText = null
            }
        }
    }

    // Swipe Gesture Seek States
    var isSwipeSeeking by remember { mutableStateOf(false) }
    var swipeSeekDeltaMs by remember { mutableLongStateOf(0L) }
    var swipeSeekTargetMs by remember { mutableLongStateOf(0L) }

    // Hold for 2X Speed
    var isHolding2x by remember { mutableStateOf(false) }

    // Screen Lock
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }
    var unlockPromptTimerKey by remember { mutableIntStateOf(0) }

    // Double Tap Seeking
    var doubleTapSide by remember { mutableStateOf<String?>(null) }
    var accumulatedSeekSeconds by remember { mutableIntStateOf(0) }
    var doubleTapTriggerKey by remember { mutableIntStateOf(0) }
    val isSkipActive = doubleTapSide != null || isSwipeSeeking || isScrubbing

    // Auto-hide unlock prompt
    LaunchedEffect(isScreenLocked, showUnlockPrompt, unlockPromptTimerKey) {
        if (isScreenLocked && showUnlockPrompt) {
            delay(3000)
            showUnlockPrompt = false
        }
    }

    // AniSkip: Skip Intro & Outro intervals
    var skipIntervals by remember(malId, episode) { mutableStateOf<List<SkipInterval>>(emptyList()) }
    LaunchedEffect(malId, episode, durationMs) {
        if (malId != null && malId > 0 && episode > 0) {
            val epSec = if (durationMs > 0L) durationMs / 1000L else 0L
            val intervals = AniSkipRepository.getSkipTimes(malId, episode, epSec)
            skipIntervals = intervals
        } else {
            skipIntervals = emptyList()
        }
    }
    val activeSkipInterval = remember(currentPositionMs, skipIntervals) {
        skipIntervals.firstOrNull { currentPositionMs in it.startTimeMs until it.endTimeMs }
    }

    // Controls visibility
    var showControls by remember { mutableStateOf(handoff != null) }   // visible immediately when adopting
    var controlsTimeoutKey by remember { mutableIntStateOf(0) }
    var isAutoHideTransition by remember { mutableStateOf(false) }
    // No player UI over the loading screen: controls appear once playback actually starts.
    val isInitialLoading = engineMode != PlayerEngineMode.IFRAME_FALLBACK && !hasStartedPlayback
    LaunchedEffect(isInitialLoading) {
        if (isInitialLoading) {
            isAutoHideTransition = false
            showControls = false
        } else {
            isAutoHideTransition = false
            showControls = true
            controlsTimeoutKey++
        }
    }
    LaunchedEffect(showControls) { if (isInitialLoading && showControls) showControls = false }   // ignore taps while loading

    // Track Menus
    var audioTracks by remember { mutableStateOf<List<AudioTrackInfo>>(emptyList()) }
    var qualityTracks by remember { mutableStateOf<List<QualityTrackInfo>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrackInfo>>(emptyList()) }
    var showCaptionModal by remember { mutableStateOf(false) }
    val glassBackdrop = remember { GlassBackdrop() }
    val useGlassBlur = Build.VERSION.SDK_INT >= 31      // Android 12+: TextureView + Haze backdrop blur
    val hazeState = rememberHazeState()
    val glassCompact = glassBackdrop.hostWidth > 0 && glassBackdrop.hostWidth / context.resources.displayMetrics.density < 560f
    var morphAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    // Caption State & Customization
    var activeLoadedCues by remember(episode, season) { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var activeCuesText by remember(episode, season) { mutableStateOf<String?>(null) }
    var isCaptionsEnabled by remember { mutableStateOf(true) }
    var captionConfig by remember { mutableStateOf(CaptionCustomization()) }
    var captionPlayStatus by remember { mutableStateOf(CaptionPlayStatus.OFF) }
    var activeCaptionTitle by remember { mutableStateOf("Off") }
    var activeLoadedSubtitleUrl by remember(episode, season) { mutableStateOf<String?>(null) }
    var captionErrorText by remember { mutableStateOf<String?>(null) }
    var downloadingSubtitleId by remember { mutableStateOf<String?>(null) }
    var captionSearchQuery by remember(title) { mutableStateOf(title) }
    var isSearchingCaptions by remember { mutableStateOf(false) }
    var searchedSubtitles by remember { mutableStateOf<List<SubtitleTrack>>(emptyList()) }
    var captionSearchMessage by remember { mutableStateOf<String?>(null) }
    var preferredSubLanguage by remember { mutableStateOf("English") }
    var preferredAudioLanguage by remember { mutableStateOf("") }
    var preferredQualityHeight by remember { mutableIntStateOf(0) }
    var hasLoadedPreferences by remember { mutableStateOf(false) }
    var selectedSubSeason by remember(season) { mutableIntStateOf(season) }
    var selectedSubEpisode by remember(episode) { mutableIntStateOf(episode) }
    var subSyncMs by remember { mutableLongStateOf(0L) }

    // ── Dialogue Panel ───────────────────────────────────────────────────────
    var showDialoguePanel by remember { mutableStateOf(false) }
    var dialogueSearchQuery by remember { mutableStateOf("") }

    // ── Real-time Translation ─────────────────────────────────────────────────
    // null = off; set to ISO lang code (e.g. "es") to enable translation
    var translationTargetLangCode by remember { mutableStateOf<String?>(null) }
    var translationTargetLangName by remember { mutableStateOf<String?>(null) }
    var translatedCueText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var lastPrefetchedCueIndex by remember { mutableIntStateOf(-1) }
    var selectedLanguageFilter by remember { mutableStateOf("All") }

    // ── Upload Subtitle File ──────────────────────────────────────────────────
    val uploadSubLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
                if (raw.isNotBlank()) {
                    val cues = SubtitleParser.parse(raw)
                    if (cues.isNotEmpty()) {
                        activeLoadedCues = cues
                        activeLoadedSubtitleUrl = "file://$uri"
                        isCaptionsEnabled = true
                        captionPlayStatus = CaptionPlayStatus.PLAYING
                        activeCaptionTitle = "Uploaded File"
                        captionErrorText = null
                        val pos = (currentPositionMs - subSyncMs + 120L).coerceAtLeast(0L)
                        val activeCues = findActiveCuesAt(cues, pos)
                        activeCuesText = if (activeCues.isNotEmpty()) activeCues.map { it.text }.distinct().joinToString("\n") else null
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Load persisted user preferences (audio language, subtitle language, quality, caption styling)
    LaunchedEffect(Unit) {
        try {
            val savedAudio = UserPreferences.getAudioLanguageSync(context)
            if (savedAudio.isNotBlank()) {
                preferredAudioLanguage = savedAudio
            }
            val savedSubLang = UserPreferences.getSubLanguageSync(context)
            if (savedSubLang.isNotBlank()) {
                preferredSubLanguage = savedSubLang
            }
            val savedHeight = UserPreferences.getPreferredQualityHeightSync(context)
            if (savedHeight > 0) {
                preferredQualityHeight = savedHeight
            }
            val savedConfig = UserPreferences.getCaptionConfig(context).first()
            captionConfig = CaptionCustomization(
                fontSizeSp = savedConfig.fontSizeSp,
                textColor = Color(savedConfig.textColorArgb),
                bgAlpha = savedConfig.bgAlpha,
                hasShadow = savedConfig.hasShadow,
                isBold = savedConfig.isBold,
                hasStroke = savedConfig.hasStroke,
                strokeColor = Color(savedConfig.strokeColorArgb),
                strokeWidth = savedConfig.strokeWidth
            )
            isCaptionsEnabled = UserPreferences.getCaptionsEnabled(context).first()
            hasLoadedPreferences = true
        } catch (_: Exception) {
            hasLoadedPreferences = true
        }
    }

    // Persist caption settings changes after initial load
    LaunchedEffect(captionConfig, isCaptionsEnabled, hasLoadedPreferences) {
        if (hasLoadedPreferences) {
            try {
                UserPreferences.saveCaptionsEnabled(context, isCaptionsEnabled)
                UserPreferences.saveCaptionConfig(
                    context = context,
                    fontSizeSp = captionConfig.fontSizeSp,
                    textColorArgb = captionConfig.textColor.toArgb(),
                    bgAlpha = captionConfig.bgAlpha,
                    hasShadow = captionConfig.hasShadow,
                    isBold = captionConfig.isBold,
                    hasStroke = captionConfig.hasStroke,
                    strokeColorArgb = captionConfig.strokeColor.toArgb(),
                    strokeWidth = captionConfig.strokeWidth
                )
            } catch (_: Exception) {}
        }
    }

    // Persist subtitle language preference
    LaunchedEffect(preferredSubLanguage, hasLoadedPreferences) {
        if (hasLoadedPreferences && preferredSubLanguage.isNotBlank()) {
            try {
                UserPreferences.saveSubLanguage(context, preferredSubLanguage)
            } catch (_: Exception) {}
        }
    }

    // Persist audio language preference
    LaunchedEffect(preferredAudioLanguage, hasLoadedPreferences) {
        if (hasLoadedPreferences && preferredAudioLanguage.isNotBlank()) {
            try {
                UserPreferences.saveAudioLanguage(context, preferredAudioLanguage)
            } catch (_: Exception) {}
        }
    }

    // Apply preferred audio track when audio tracks become available or preference changes
    LaunchedEffect(preferredAudioLanguage, audioTracks) {
        val p = exoPlayer ?: return@LaunchedEffect
        if (preferredAudioLanguage.isNotBlank() && audioTracks.isNotEmpty()) {
            val targetAudio = audioTracks.firstOrNull {
                it.language.equals(preferredAudioLanguage, ignoreCase = true) ||
                it.label.equals(preferredAudioLanguage, ignoreCase = true) ||
                it.label.contains(preferredAudioLanguage, ignoreCase = true)
            }
            if (targetAudio != null && !targetAudio.isSelected) {
                try {
                    val grp = p.currentTracks.groups[targetAudio.groupIndex]
                    val lang = if (targetAudio.language.isNotBlank() && targetAudio.language != "und") targetAudio.language else null
                    p.trackSelectionParameters = p.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .setOverrideForType(
                            TrackSelectionOverride(grp.mediaTrackGroup, targetAudio.trackIndex)
                        )
                        .setPreferredAudioLanguage(lang)
                        .build()
                } catch (_: Exception) {}
            }
        }
    }

    // Persist quality preference
    LaunchedEffect(preferredQualityHeight, hasLoadedPreferences) {
        if (hasLoadedPreferences && preferredQualityHeight > 0) {
            try {
                UserPreferences.savePreferredQuality(context, preferredQualityHeight, "${preferredQualityHeight}p")
            } catch (_: Exception) {}
        }
    }

    // Auto-load / Auto-download matching subtitles whenever episode, season, or externalSubtitles update
    LaunchedEffect(episode, season, externalSubtitles) {
        if (!isCaptionsEnabled) {
            activeLoadedCues = emptyList()
            activeLoadedSubtitleUrl = null
            activeCuesText = null
            captionPlayStatus = CaptionPlayStatus.OFF
            return@LaunchedEffect
        }

        // 1. Clear stale cues from previous episode
        activeLoadedCues = emptyList()
        activeLoadedSubtitleUrl = null
        activeCuesText = null
        captionErrorText = null

        // Sync modal pickers to match current episode
        selectedSubSeason = season
        selectedSubEpisode = episode

        // 2. Find best candidate from externalSubtitles (prioritizing Direct official stream captions) or fetch directly
        var candidateTrack = externalSubtitles.firstOrNull {
            (it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)) &&
            (it.source.startsWith("Direct") || it.source == "Anikoto")
        } ?: externalSubtitles.firstOrNull {
            it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)
        } ?: externalSubtitles.firstOrNull {
            (it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)) &&
            (it.source.startsWith("Direct") || it.source == "Anikoto")
        } ?: externalSubtitles.firstOrNull {
            it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)
        } ?: externalSubtitles.firstOrNull()

        if (candidateTrack == null && (anilistId > 0 || title.isNotBlank())) {
            captionPlayStatus = CaptionPlayStatus.DOWNLOADING
            try {
                val fetched = SubtitleRepository.fetchAllSubtitles(
                    anilistId = anilistId,
                    title = title,
                    episode = episode,
                    season = season,
                    audio = if (isDub) "dub" else "sub"
                )
                candidateTrack = fetched.firstOrNull {
                    (it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)) &&
                    (it.source.startsWith("Direct") || it.source == "Anikoto")
                } ?: fetched.firstOrNull {
                    it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)
                } ?: fetched.firstOrNull {
                    (it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)) &&
                    (it.source.startsWith("Direct") || it.source == "Anikoto")
                } ?: fetched.firstOrNull {
                    it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)
                } ?: fetched.firstOrNull()
            } catch (_: Exception) {}
        }

        if (candidateTrack != null) {
            captionPlayStatus = CaptionPlayStatus.DOWNLOADING
            downloadingSubtitleId = candidateTrack.id

            val dlResult = SubtitleRepository.downloadSubtitleFile(context, candidateTrack)
            dlResult.onSuccess { file ->
                try {
                    val raw = file.readText(Charsets.UTF_8)
                    val cues = SubtitleParser.parse(raw)
                    if (cues.isNotEmpty()) {
                        activeLoadedCues = cues
                        activeLoadedSubtitleUrl = candidateTrack.url
                        activeCaptionTitle = candidateTrack.label
                        captionPlayStatus = CaptionPlayStatus.PLAYING
                        captionErrorText = null
                        val pos = (currentPositionMs - subSyncMs + 120L).coerceAtLeast(0L)
                        val activeCues = findActiveCuesAt(cues, pos)
                        activeCuesText = if (activeCues.isNotEmpty()) activeCues.map { it.text }.distinct().joinToString("\n") else null
                    } else {
                        captionPlayStatus = CaptionPlayStatus.ERROR
                        captionErrorText = "No subtitle dialogue found in file"
                    }
                } catch (e: Exception) {
                    captionPlayStatus = CaptionPlayStatus.ERROR
                    captionErrorText = "Failed to parse subtitles for Ep $episode"
                } finally {
                    downloadingSubtitleId = null
                }
            }.onFailure { err ->
                captionPlayStatus = CaptionPlayStatus.ERROR
                captionErrorText = err.message ?: "Failed to download subtitles for Ep $episode"
                downloadingSubtitleId = null
            }
        } else {
            captionPlayStatus = CaptionPlayStatus.OFF
        }
    }

    // ── Real-time & Lookahead Translation: instant 0ms retrieval with prefetch ──
    LaunchedEffect(activeCuesText, translationTargetLangCode) {
        val code = translationTargetLangCode
        val raw = activeCuesText
        if (code == null || raw == null) {
            translatedCueText = null
            isTranslating = false
            return@LaunchedEffect
        }
        // 1. Instant Cache Check (0ms latency, zero delay!)
        val cached = TranslationService.getCached(raw, code)
        if (cached != null) {
            translatedCueText = cached
            isTranslating = false
        } else {
            // 2. Cache miss (e.g. user jumped to a new position)
            isTranslating = true
            translatedCueText = TranslationService.translate(raw, code) ?: raw
            isTranslating = false
        }
    }

    // Pre-fetch lookahead as soon as translation language is selected or subtitles loaded
    LaunchedEffect(translationTargetLangCode, activeLoadedCues) {
        val code = translationTargetLangCode
        if (code != null && activeLoadedCues.isNotEmpty()) {
            val pos = (currentPositionMs - subSyncMs).coerceAtLeast(0L)
            val currentCueIdx = findCueIndexAt(activeLoadedCues, pos).coerceAtLeast(0)
            lastPrefetchedCueIndex = currentCueIdx
            scope.launch(Dispatchers.IO) {
                TranslationService.prefetchUpcomingCues(
                    cues = activeLoadedCues,
                    currentIndex = currentCueIdx,
                    lookaheadCount = 6,
                    targetLangCode = code
                )
            }
        }
    }

    // Double-tap visual feedback
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }

    // Volume & Brightness HUD & Gesture States
    val activity = remember(context) { PlayerGesturePlugin.findActivity(context) }
    var currentVolumeFraction by remember { mutableFloatStateOf(PlayerGesturePlugin.getVolumeFraction(context)) }
    var currentBrightnessFraction by remember { mutableFloatStateOf(PlayerGesturePlugin.getBrightnessFraction(activity)) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var showBrightnessHud by remember { mutableStateOf(false) }
    var isInteractingVolume by remember { mutableStateOf(false) }
    var isInteractingBrightness by remember { mutableStateOf(false) }
    var hudResetTrigger by remember { mutableIntStateOf(0) }

    // Auto-hide Volume & Brightness HUDs after 1.5 seconds
    LaunchedEffect(hudResetTrigger, showVolumeHud, showBrightnessHud) {
        if (showVolumeHud || showBrightnessHud) {
            delay(1500)
            showVolumeHud = false
            showBrightnessHud = false
        }
    }

    // Hardware Volume Button & Broadcast Receiver listener
    DisposableEffect(context) {
        PlayerGesturePlugin.VolumeNotifier.isPlayerActive = true
        PlayerGesturePlugin.VolumeNotifier.onVolumeChanged = { newVol ->
            currentVolumeFraction = newVol
            showVolumeHud = true
            showBrightnessHud = false
            hudResetTrigger++
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val vol = PlayerGesturePlugin.getVolumeFraction(context)
                    currentVolumeFraction = vol
                    showVolumeHud = true
                    showBrightnessHud = false
                    hudResetTrigger++
                }
            }
        }
        context.registerReceiver(receiver, filter)

        onDispose {
            PlayerGesturePlugin.VolumeNotifier.isPlayerActive = false
            PlayerGesturePlugin.VolumeNotifier.onVolumeChanged = null
            (context as? ComponentActivity)?.let { act ->
                MainActivity.updatePipParams(act, false)
            }
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying) {
        (context as? ComponentActivity)?.let { act ->
            val vSize = exoPlayer?.videoSize
            val rat = if (vSize != null && vSize.width > 0 && vSize.height > 0) {
                android.util.Rational(vSize.width, vSize.height)
            } else {
                android.util.Rational(16, 9)
            }
            MainActivity.updatePipParams(act, isPlaying, rat)
        }
    }

    LaunchedEffect(isInPip) {
        if (isInPip) {
            showControls = false
        } else {
            val act = context as? ComponentActivity
            if (act != null && (act.isFinishing || act.lifecycle.currentState < androidx.lifecycle.Lifecycle.State.RESUMED)) {
                exoPlayer?.pause()
                isPlaying = false
            }
        }
    }

    // Auto-hide controls after user configured timeout (suppressed during active skip/seek/scrub)
    LaunchedEffect(showControls, controlsTimeoutKey, isPlaying, playerCustomization.glass.autoHideTimeoutSeconds, isSkipActive) {
        val timeoutSec = playerCustomization.glass.autoHideTimeoutSeconds
        if (showControls && isPlaying && timeoutSec > 0 && !isSkipActive) {
            delay(timeoutSec * 1000L)
            isAutoHideTransition = true
            showControls = false
        }
    }

    // Stream Sniffer for Embed URLs
    LaunchedEffect(streamUrl) {
        // Mini player handoff: same episode/stream already playing -> adopt it, skip sniffing
        MiniPlayerManager.peek(anilistId, episode, streamUrl)?.let { reuse ->
            engineMode = PlayerEngineMode.NATIVE_EXO
            sniffedStream = reuse.sniffed
            return@LaunchedEffect
        }
        // Stop previous server playback immediately
        exoPlayer?.let { p ->
            p.pause()
            p.stop()
            p.clearMediaItems()
        }

        if (isDirectStream) {
            engineMode = PlayerEngineMode.NATIVE_EXO
            sniffedStream = SniffedStream(
                streamUrl = streamUrl,
                isHls = streamUrl.contains(".m3u8", ignoreCase = true),
                headers = headers,
                subtitles = externalSubtitles
            )
        } else {
            // Sniff stream for native ExoPlayer playback
            engineMode = PlayerEngineMode.SNIFFING
            val sniffed = StreamSniffer.sniff(context, streamUrl)
            if (sniffed != null) {
                sniffedStream = sniffed
                engineMode = PlayerEngineMode.NATIVE_EXO
            } else {
                // Gracefully fallback to Iframe player without jumping servers during URL extraction
                engineMode = PlayerEngineMode.IFRAME_FALLBACK
            }
        }
    }

    // Stop ExoPlayer when switched away from NATIVE_EXO
    LaunchedEffect(engineMode) {
        if (engineMode != PlayerEngineMode.NATIVE_EXO) {
            exoPlayer?.let { p ->
                p.pause()
                p.stop()
                p.clearMediaItems()
            }
        }
    }

    // Initialize ExoPlayer when NATIVE_EXO is active
    LaunchedEffect(engineMode, sniffedStream) {
        if (engineMode == PlayerEngineMode.NATIVE_EXO && sniffedStream != null) {
            val adopted = MiniPlayerManager.claim(anilistId, episode, streamUrl)
            if (adopted == null) {
                exoPlayer?.release()
                listenerRefs.clear()
            }

            val stream = sniffedStream ?: return@LaunchedEffect
            val streamHost = try { Uri.parse(stream.streamUrl).host.orEmpty() } catch (_: Exception) { "" }
            val embedUri = try { Uri.parse(streamUrl) } catch (_: Exception) { null }
            val embedHost = embedUri?.host.orEmpty()
            val isVidnestStream = embedHost.contains("vidnest") || streamHost.contains("vidnest") || stream.streamUrl.contains("vidnest") || streamUrl.contains("vidnest")
            val isMegaplayStream = embedHost.contains("megaplay") || streamHost.contains("megaplay") || stream.streamUrl.contains("megaplay") || streamUrl.contains("megaplay")

            val effectiveHeaders = stream.headers.toMutableMap()
            effectiveHeaders.putAll(headers)
            effectiveHeaders["User-Agent"] = headers["User-Agent"] ?: stream.headers["User-Agent"] ?: StreamSniffer.TARGET_UA
            if (isVidnestStream) {
                if (!effectiveHeaders.containsKey("Referer")) effectiveHeaders["Referer"] = "https://vidnest.fun/"
                if (!effectiveHeaders.containsKey("Origin")) effectiveHeaders["Origin"] = "https://vidnest.fun"
            } else if (isMegaplayStream) {
                if (!effectiveHeaders.containsKey("Referer")) effectiveHeaders["Referer"] = "https://megaplay.buzz/"
                if (!effectiveHeaders.containsKey("Origin")) effectiveHeaders["Origin"] = "https://megaplay.buzz"
            } else {
                if (!effectiveHeaders.containsKey("Referer")) {
                    effectiveHeaders["Referer"] = if (embedHost.isNotBlank()) "https://$embedHost/" else if (streamHost.isNotBlank()) "https://$streamHost/" else "https://vidnest.fun/"
                }
                if (!effectiveHeaders.containsKey("Origin")) {
                    effectiveHeaders["Origin"] = if (embedHost.isNotBlank()) "https://$embedHost" else if (streamHost.isNotBlank()) "https://$streamHost" else "https://vidnest.fun"
                }
            }

            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(StreamSniffer.TARGET_UA)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true)

            httpFactory.setDefaultRequestProperties(effectiveHeaders)

            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)

            // High-speed tuned LoadControl matching HLS.js lowLatencyMode & fast buffer
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 15_000,
                    /* maxBufferMs = */ 45_000,
                    /* bufferForPlaybackMs = */ 250,
                    /* bufferForPlaybackAfterRebufferMs = */ 500
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(30_000, false)
                .build()

            val player = adopted?.player ?: ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .build()
            if (adopted != null) hasAppliedInitialSeek = true // keep adopted position, don't re-seek to saved pos

            if (adopted == null) {
            // Initialize audio track selection preference based on dub/sub mode
            val initialAudioLang = if (isDub) "en" else "ja"
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setPreferredAudioLanguage(initialAudioLang)
                .build()

            val allSubs = (externalSubtitles + stream.subtitles).distinctBy { it.url }
            val subtitleConfigs = allSubs.map { sub ->
                val mimeType = when {
                    sub.url.contains(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                    sub.url.contains(".ass", ignoreCase = true) || sub.url.contains(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                    else -> MimeTypes.TEXT_VTT
                }
                val subUri = if (sub.url.startsWith("/")) {
                    Uri.fromFile(java.io.File(sub.url))
                } else {
                    Uri.parse(sub.url)
                }
                MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(mimeType)
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }

            // Playback via local client proxy for remote streams (bypasses 403, 404, CORS and CDN restrictions)
            val playUrl = stream.streamUrl
            val isLocalFile = playUrl.startsWith("/") || playUrl.startsWith("file://")
            val isAlreadyProxy = playUrl.startsWith("http://127.0.0.1") || playUrl.startsWith("http://localhost")
            val finalPlayUrl = if (isLocalFile || isAlreadyProxy) {
                playUrl
            } else {
                StreamProxyServer.buildProxyUrl(playUrl, effectiveHeaders)
            }
            val mediaUri = if (isLocalFile) {
                val cleanPath = playUrl.removePrefix("file://")
                Uri.fromFile(java.io.File(cleanPath))
            } else {
                Uri.parse(finalPlayUrl)
            }

            val mediaItemBuilder = MediaItem.Builder().setUri(mediaUri)
            val isHlsStream = !isLocalFile && (stream.isHls || playUrl.contains(".m3u8", ignoreCase = true))
            if (playUrl.endsWith(".ts", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
            } else if (playUrl.endsWith(".mp4", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
            } else if (isHlsStream) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            if (subtitleConfigs.isNotEmpty()) {
                mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
            }
            val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title)
                .setArtist("Episode $episode" + (episodeTitle?.let { " - $it" } ?: ""))
                .setArtworkUri(nextEpisodeImage?.let { Uri.parse(it) })
                .build()
            mediaItemBuilder.setMediaMetadata(mediaMetadata)
            val mediaItem = mediaItemBuilder.build()

            if (isHlsStream) {
                // Chunkless preparation for instant start matching Hls.js in test_shaka_player.html
                val hlsSource = androidx.media3.exoplayer.hls.HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
                player.setMediaSource(hlsSource)
            } else {
                player.setMediaItem(mediaItem)
            }
            player.setPlaybackSpeed(1.0f)
            }

            val playerListener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                    isPlaying = PlayerPlaybackPlugin.isPlayingOrIntended(player)
                    if (state == Player.STATE_READY) {
                        hasStartedPlayback = true
                        durationMs = player.duration.coerceAtLeast(0L)
                        if (!hasAppliedInitialSeek) {
                            hasAppliedInitialSeek = true
                            val isNearEnd = durationMs > 0L && (initialTimeMs >= durationMs - 25_000L || initialTimeMs >= (durationMs * 0.90f).toLong())
                            if (initialTimeMs > 2000L && !isNearEnd) {
                                PlayerPlaybackPlugin.seekTo(player, initialTimeMs)
                                val sec = (initialTimeMs / 1000) % 60
                                val min = (initialTimeMs / (1000 * 60)) % 60
                                val hr = initialTimeMs / (1000 * 60 * 60)
                                val timeStr = if (hr > 0) String.format(Locale.US, "%d:%02d:%02d", hr, min, sec) else String.format(Locale.US, "%d:%02d", min, sec)
                                resumeToastText = "Resumed from $timeStr"
                                scope.launch {
                                    delay(2800)
                                    resumeToastText = null
                                }
                            }
                        }
                    } else if (state == Player.STATE_ENDED) {
                        isPlaying = false
                        // Auto Play Next Episode: ensure playback actually reached end after real playback
                        val validEnded = (player.duration > 10_000L && player.currentPosition > 5_000L) || (durationMs > 10_000L && currentPositionMs > 5_000L)
                        if (exoPlayer === player && hasNextNow && autoPlayNextEnabled && !isNextTriggered && !isUpNextCanceled && validEnded) {
                            isNextTriggered = true
                            onNextNow()
                        }
                    }
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    isPlaying = PlayerPlaybackPlugin.isPlayingOrIntended(player)
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = PlayerPlaybackPlugin.isPlayingOrIntended(player)
                    if (!playing) {
                        com.nyyrox.aniverse.data.discord.DiscordRpcService.pausePresence()
                    }
                }

                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        (context as? ComponentActivity)?.let { act ->
                            MainActivity.updatePipParams(
                                act,
                                PlayerPlaybackPlugin.isPlayingOrIntended(player),
                                android.util.Rational(videoSize.width, videoSize.height)
                            )
                        }
                    }
                }

                override fun onCues(cueGroup: CueGroup) {
                    if (activeLoadedCues.isEmpty() && isCaptionsEnabled) {
                        val rawCues = cueGroup.cues.mapNotNull { it.text?.toString() }
                            .joinToString("\n")
                            .trim()
                        activeCuesText = if (rawCues.isNotBlank()) rawCues else null
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    val audios = mutableListOf<AudioTrackInfo>()
                    val qualities = mutableListOf<QualityTrackInfo>()

                    for (gIdx in 0 until tracks.groups.size) {
                        val group = tracks.groups[gIdx]
                        val trackType = group.type

                        if (trackType == C.TRACK_TYPE_AUDIO) {
                            for (tIdx in 0 until group.length) {
                                val format = group.getTrackFormat(tIdx)
                                val lang = format.language ?: "und"
                                val label = formatAudioTrackLabel(format, audios.size)
                                audios.add(
                                    AudioTrackInfo(
                                        groupIndex = gIdx,
                                        trackIndex = tIdx,
                                        label = label,
                                        language = lang,
                                        isSelected = group.isTrackSelected(tIdx)
                                    )
                                )
                            }
                        } else if (trackType == C.TRACK_TYPE_VIDEO) {
                            for (tIdx in 0 until group.length) {
                                val format = group.getTrackFormat(tIdx)
                                val h = format.height
                                val label = if (h > 0) "${h}p" else "Stream ${qualities.size + 1}"
                                qualities.add(
                                    QualityTrackInfo(
                                        groupIndex = gIdx,
                                        trackIndex = tIdx,
                                        height = h,
                                        bitrate = format.bitrate,
                                        label = label,
                                        isSelected = group.isTrackSelected(tIdx)
                                    )
                                )
                            }
                        }
                    }

                    audioTracks = audios
                    qualityTracks = qualities.sortedByDescending { it.height }
                    subtitleTracks = emptyList() // HLS stream tracks omitted per user instruction

                    // Auto-apply audio track preference (User selection priority > DUB vs SUB defaults)
                    if (audios.isNotEmpty()) {
                        val targetAudio = if (preferredAudioLanguage.isNotBlank()) {
                            audios.firstOrNull {
                                it.language.equals(preferredAudioLanguage, ignoreCase = true) ||
                                it.label.equals(preferredAudioLanguage, ignoreCase = true) ||
                                it.label.contains(preferredAudioLanguage, ignoreCase = true)
                            }
                        } else if (isDub) {
                            // In DUB mode: default to English track if user hasn't explicitly selected
                            audios.firstOrNull {
                                it.language.startsWith("en", ignoreCase = true) ||
                                it.label.contains("english", ignoreCase = true) ||
                                it.label.contains("dub", ignoreCase = true)
                            }
                        } else {
                            // In SUB mode: default to Japanese/Native track
                            audios.firstOrNull {
                                it.language.startsWith("ja", ignoreCase = true) ||
                                it.label.contains("japanese", ignoreCase = true) ||
                                it.label.contains("native", ignoreCase = true)
                            }
                        }

                        if (targetAudio != null && !targetAudio.isSelected) {
                            try {
                                val grp = tracks.groups[targetAudio.groupIndex]
                                val lang = if (targetAudio.language.isNotBlank() && targetAudio.language != "und") targetAudio.language else null
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                    .setOverrideForType(
                                        TrackSelectionOverride(grp.mediaTrackGroup, targetAudio.trackIndex)
                                    )
                                    .setPreferredAudioLanguage(lang)
                                    .build()
                            } catch (_: Exception) {}
                        }
                    }

                    // Auto-apply saved video quality preference if available
                    if (preferredQualityHeight > 0 && qualities.isNotEmpty()) {
                        val matchingQuality = qualities.firstOrNull { it.height == preferredQualityHeight }
                            ?: qualities.filter { it.height in 1..preferredQualityHeight }.maxByOrNull { it.height }
                        if (matchingQuality != null && !matchingQuality.isSelected) {
                            try {
                                val grp = tracks.groups[matchingQuality.groupIndex]
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .setOverrideForType(
                                        TrackSelectionOverride(grp.mediaTrackGroup, matchingQuality.trackIndex)
                                    )
                                    .build()
                            } catch (_: Exception) {}
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val codeName = error.errorCodeName
                    val code = error.errorCode
                    val cause = error.cause
                    val issueMsg = when {
                        cause is HttpDataSource.InvalidResponseCodeException ->
                            "HTTP ${cause.responseCode}: ${cause.message ?: "Forbidden / Access Denied by Server"}"
                        cause is HttpDataSource.HttpDataSourceException ->
                            cause.message ?: "HTTP Data Source Network Issue"
                        cause != null && !cause.message.isNullOrBlank() -> cause.message!!
                        !error.message.isNullOrBlank() -> error.message!!
                        else -> "Playback failed: stream unreachable or format unsupported."
                    }
                    playbackErrorCode = "$codeName ($code)"
                    playbackErrorInfo = issueMsg
                    android.util.Log.e("CinePlayerView", "Player error: $codeName ($code): $issueMsg", error)

                    // If ExoPlayer fails on a sniffed stream (e.g. HTTP 403 / 2004 or CDN CORS token expired), fallback to Iframe player
                    if (engineMode == PlayerEngineMode.NATIVE_EXO && sniffedStream != null && streamUrl != sniffedStream?.streamUrl) {
                        android.util.Log.w("CinePlayerView", "Falling back to IFRAME_FALLBACK due to stream error: $issueMsg")
                        engineMode = PlayerEngineMode.IFRAME_FALLBACK
                        return
                    }

                    onPlaybackFailed()
                }
            }
            player.addListener(playerListener)
            listenerRefs.add(playerListener)

            if (adopted == null) {
                player.prepare()
                player.playWhenReady = !startPaused
            }
            exoPlayer = player
            if (adopted != null) {
                // replay state the new listener missed while the player lived in the mini player
                playerListener.onTracksChanged(player.currentTracks)
                playerListener.onPlaybackStateChanged(player.playbackState)
                playerListener.onIsPlayingChanged(player.isPlaying)
                val vs = player.videoSize
                if (vs.width > 0 && vs.height > 0) playerListener.onVideoSizeChanged(vs)
                // paused adopt: force a frame onto the new surface
                if (!player.playWhenReady) PlayerPlaybackPlugin.seekTo(player, player.currentPosition)
            }
            com.nyyrox.aniverse.service.ActivePlayerHolder.attach(
                exoPlayer = player,
                skipNext = if (hasNextEpisode) onNextEpisode else null,
                skipPrev = if (hasPreviousEpisode) onPreviousEpisode else null,
                hasNextEp = hasNextEpisode,
                hasPrevEp = hasPreviousEpisode
            )
            val attachIntent = Intent(context, com.nyyrox.aniverse.service.PlayerNotificationService::class.java).apply {
                action = com.nyyrox.aniverse.service.PlayerNotificationService.ACTION_ATTACH
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { context.startForegroundService(attachIntent) } catch (_: Exception) { context.startService(attachIntent) }
            } else {
                context.startService(attachIntent)
            }
        }
    }

    LaunchedEffect(exoPlayer, hasNextEpisode, hasPreviousEpisode) {
        exoPlayer?.let {
            com.nyyrox.aniverse.service.ActivePlayerHolder.updateNavigation(
                hasNextEp = hasNextEpisode,
                hasPrevEp = hasPreviousEpisode,
                skipNext = if (hasNextEpisode) onNextEpisode else null,
                skipPrev = if (hasPreviousEpisode) onPreviousEpisode else null
            )
            val updateIntent = Intent(context, com.nyyrox.aniverse.service.PlayerNotificationService::class.java).apply {
                action = com.nyyrox.aniverse.service.PlayerNotificationService.ACTION_UPDATE
            }
            try { context.startService(updateIntent) } catch (_: Exception) {}
        }
    }

    // Periodic time tick & progress reporting & subtitle cue sync & auto-skip intro/outro
    LaunchedEffect(exoPlayer, engineMode, initialTimeMs, activeLoadedCues, isCaptionsEnabled, subSyncMs, autoSkipIntroOutroEnabled, skipIntervals) {
        var lastProgressReport = 0L
        var lastAutoSkippedKey: String? = null
        var lastUiTimeSyncMs = 0L
        while (engineMode == PlayerEngineMode.NATIVE_EXO) {
            exoPlayer?.let { p ->
                val livePos = p.currentPosition.coerceAtLeast(0L)
                val now = System.currentTimeMillis()

                if (!hasAppliedInitialSeek && p.playbackState == Player.STATE_READY) {
                    hasAppliedInitialSeek = true
                    val dur = p.duration.coerceAtLeast(0L)
                    val isNearEnd = dur > 0L && (initialTimeMs >= dur - 25_000L || initialTimeMs >= (dur * 0.90f).toLong())
                    if (initialTimeMs > 2000L && !isNearEnd) {
                        PlayerPlaybackPlugin.seekTo(p, initialTimeMs)
                    }
                }

                // Update UI progress/seekbar throttled to 100ms for silky UI performance
                if (now - lastUiTimeSyncMs >= 100L || kotlin.math.abs(livePos - currentPositionMs) > 500L) {
                    currentPositionMs = livePos
                    bufferedPositionMs = p.bufferedPosition.coerceAtLeast(0L)
                    lastUiTimeSyncMs = now
                    if (p.duration > 0) {
                        durationMs = p.duration
                        if (livePos > 0L && (now - lastProgressReport >= 1000L)) {
                            onProgressUpdate(livePos, durationMs)
                            lastProgressReport = now
                        }
                    }
                }

                // Auto-skip Intro / Outro when enabled
                if (autoSkipIntroOutroEnabled && skipIntervals.isNotEmpty() && !isScrubbing && !isSwipeSeeking) {
                    val matchingInterval = skipIntervals.firstOrNull { livePos in it.startTimeMs until it.endTimeMs }
                    if (matchingInterval != null) {
                        val key = "${matchingInterval.startTimeMs}_${matchingInterval.endTimeMs}"
                        if (lastAutoSkippedKey != key) {
                            lastAutoSkippedKey = key
                            PlayerPlaybackPlugin.seekTo(p, matchingInterval.endTimeMs + 200L)
                            val skipLabel = when (matchingInterval.skipType.lowercase(Locale.US)) {
                                "op", "mixed-op" -> "Intro"
                                "ed", "mixed-ed" -> "Outro"
                                else -> "Recap"
                            }
                            resumeToastText = "Auto-skipped $skipLabel ⚡"
                            scope.launch {
                                delay(2200)
                                if (resumeToastText == "Auto-skipped $skipLabel ⚡") {
                                    resumeToastText = null
                                }
                            }
                        }
                    }
                }

                // Subtitle sync with parsed cues (adjusted by subSyncMs offset + 120ms lead compensation)
                if (isCaptionsEnabled && activeLoadedCues.isNotEmpty()) {
                    // 120ms audio-visual lead aligns spoken audio onset with visual text presentation, matching web player
                    val pos = (livePos - subSyncMs + 120L).coerceAtLeast(0L)
                    val activeCues = findActiveCuesAt(activeLoadedCues, pos)
                    val newCueText = if (activeCues.isNotEmpty()) {
                        activeCues.map { it.text }.distinct().joinToString("\n")
                    } else null
                    if (activeCuesText != newCueText) {
                        activeCuesText = newCueText
                    }

                    // Lookahead Translation: pre-translate 4-5 cues ahead in the background!
                    val currentCueIdx = findCueIndexAt(activeLoadedCues, pos)
                    val targetLang = translationTargetLangCode
                    if (targetLang != null && currentCueIdx >= 0 && currentCueIdx != lastPrefetchedCueIndex) {
                        lastPrefetchedCueIndex = currentCueIdx
                        scope.launch(Dispatchers.IO) {
                            TranslationService.prefetchUpcomingCues(
                                cues = activeLoadedCues,
                                currentIndex = currentCueIdx,
                                lookaheadCount = 5,
                                targetLangCode = targetLang
                            )
                        }
                    }
                } else if (!isCaptionsEnabled) {
                    if (activeCuesText != null) {
                        activeCuesText = null
                    }
                }
            }

            // High-frequency 20ms refresh tick when playing with captions enabled for zero-latency frame sync;
            // power-saving 100ms when paused or without active captions
            val isFastSync = isCaptionsEnabled && activeLoadedCues.isNotEmpty() && (exoPlayer?.isPlaying == true)
            delay(if (isFastSync) 20L else 100L)
        }
    }

    val performPark = remember(anilistId, episode, streamUrl, title, episodeTitle, engineMode) {
        {
            val p0 = exoPlayer
            val st = sniffedStream
            val req = MiniPlayerManager.freshRequest() ?: MiniPlayerManager.ParkRequest(anilistId, episode, null)
            if (p0 != null && st != null && engineMode == PlayerEngineMode.NATIVE_EXO &&
                (p0.playbackState == Player.STATE_READY || p0.playbackState == Player.STATE_BUFFERING) &&
                p0.playerError == null) {
                val pos = p0.currentPosition.coerceAtLeast(0L)
                val dur = p0.duration.coerceAtLeast(0L)
                if (pos > 0L) onProgressUpdate(pos, dur)
                listenerRefs.forEach { p0.removeListener(it) }
                listenerRefs.clear()
                com.nyyrox.aniverse.service.ActivePlayerHolder.updateNavigation(false, false, null, null)
                try {
                    context.startService(Intent(context, com.nyyrox.aniverse.service.PlayerNotificationService::class.java).apply {
                        action = com.nyyrox.aniverse.service.PlayerNotificationService.ACTION_UPDATE
                    })
                } catch (_: Exception) {}
                com.nyyrox.aniverse.data.discord.DiscordRpcService.clearPresence()
                MiniPlayerManager.park(
                    MiniPlayerManager.Parked(
                        player = p0,
                        routeAnimeId = req.routeAnimeId,
                        anilistId = anilistId,
                        episode = episode,
                        providerId = req.providerId,
                        streamUrl = streamUrl,
                        sniffed = st,
                        title = title,
                        subtitle = "Episode $episode" + (episodeTitle?.let { " - $it" } ?: ""),
                        onProgress = onProgressUpdate
                    ),
                    context
                )
                exoPlayer = null
            }
        }
    }

    LaunchedEffect(performPark, exoPlayer, sniffedStream, engineMode) {
        if (exoPlayer != null && sniffedStream != null && engineMode == PlayerEngineMode.NATIVE_EXO) {
            MiniPlayerManager.parkNow = performPark
        } else {
            MiniPlayerManager.parkNow = null
        }
    }

    // Clean up on dispose & save final timestamp (or hand the player to the mini player)
    DisposableEffect(streamUrl) {
        onDispose {
            MiniPlayerManager.parkNow = null
            val p0 = exoPlayer
            if (p0 != null && MiniPlayerManager.parked?.player !== p0) {
                val req = MiniPlayerManager.freshRequest()
                val st = sniffedStream
                val canPark = req != null && req.episode == episode && st != null &&
                    engineMode == PlayerEngineMode.NATIVE_EXO &&
                    (p0.playbackState == Player.STATE_READY || p0.playbackState == Player.STATE_BUFFERING) &&
                    p0.playerError == null
                exoPlayer = null

                if (canPark && st != null && req != null) {
                    val pos = p0.currentPosition.coerceAtLeast(0L)
                    val dur = p0.duration.coerceAtLeast(0L)
                    if (pos > 0L) onProgressUpdate(pos, dur)
                    listenerRefs.forEach { p0.removeListener(it) }
                    listenerRefs.clear()
                    com.nyyrox.aniverse.service.ActivePlayerHolder.updateNavigation(false, false, null, null)
                    try {
                        context.startService(Intent(context, com.nyyrox.aniverse.service.PlayerNotificationService::class.java).apply {
                            action = com.nyyrox.aniverse.service.PlayerNotificationService.ACTION_UPDATE
                        })
                    } catch (_: Exception) {}
                    com.nyyrox.aniverse.data.discord.DiscordRpcService.clearPresence()
                    MiniPlayerManager.park(
                        MiniPlayerManager.Parked(
                            player = p0,
                            routeAnimeId = req.routeAnimeId,
                            anilistId = anilistId,
                            episode = episode,
                            providerId = req.providerId,
                            streamUrl = streamUrl,
                            sniffed = st,
                            title = title,
                            subtitle = "Episode $episode" + (episodeTitle?.let { " - $it" } ?: ""),
                            onProgress = onProgressUpdate
                        ),
                        context
                    )
                } else {
                    MiniPlayerManager.parkRequest = null
                    com.nyyrox.aniverse.service.ActivePlayerHolder.detach()
                    val stopIntent = Intent(context, com.nyyrox.aniverse.service.PlayerNotificationService::class.java).apply {
                        action = com.nyyrox.aniverse.service.PlayerNotificationService.ACTION_DETACH
                    }
                    try { context.startService(stopIntent) } catch (_: Exception) {}
                    com.nyyrox.aniverse.data.discord.DiscordRpcService.clearPresence()
                    val pos = p0.currentPosition.coerceAtLeast(0L)
                    val dur = p0.duration.coerceAtLeast(0L)
                    if (pos > 0L) {
                        onProgressUpdate(pos, dur)
                    }
                    try {
                        p0.stop()
                        p0.release()
                    } catch (_: Exception) {}
                    com.nyyrox.aniverse.data.discord.DiscordRpcService.clearPresence()
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalGlassBackdrop provides glassBackdrop,
        LocalGlassCompact provides glassCompact,
        LocalGlassHaze provides (if (useGlassBlur) hazeState else null)
    ) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (engineMode) {
            PlayerEngineMode.SNIFFING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CineStreamLoader(
                        accentColor = playerCustomization.accentColor.color,
                        subtitle = if (isDirectStream) "Direct · HD" else null
                    )
                }
            }

            PlayerEngineMode.NATIVE_EXO -> {
                val playerPrismalBackdrop = com.styropyr0.prismal.sources.rememberPrismalGlassLayer()
                var exoSurfaceCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                CompositionLocalProvider(
                    com.styropyr0.prismal.LocalPrismalBackdrop provides playerPrismalBackdrop
                ) {
                    // ExoPlayer Surface Container
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { exoSurfaceCoordinates = it }
                        .pointerInput(isScreenLocked, isInPip) {
                            var lastTapTime = 0L
                            var lastTapX = 0f
                            var singleTapJob: kotlinx.coroutines.Job? = null

                            awaitEachGesture {
                                if (isInPip) return@awaitEachGesture
                                try {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (isScreenLocked) {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                try { change.consume() } catch (_: Exception) {}
                                                showUnlockPrompt = true
                                                unlockPromptTimerKey++
                                                break
                                            }
                                        }
                                        return@awaitEachGesture
                                    }

                                    val startX = down.position.x
                                    val startY = down.position.y
                                    val rootStartY = exoSurfaceCoordinates?.localToRoot(down.position)?.y ?: down.position.y
                                    val isRightSide = startX > size.width * 0.55f
                                    val isLeftSide = startX < size.width * 0.45f
                                    var hasDragged = false
                                    var dragMode = 0 // 0: undecided, 1: horizontal seek, 2: volume, 3: brightness, 4: drag down collapse
                                    var lastDragYDown = 0f
                                    val touchSlopPx = 14.dp.toPx()
                                    val initialSeekPosMs = currentPositionMs

                                    val startVol = PlayerGesturePlugin.getVolumeFraction(context)
                                    val startBright = PlayerGesturePlugin.getBrightnessFraction(activity)
                                    var initialPinchSpan = -1f
                                    var pinchTriggered = false
                                    var twoFingerStartY = -1f

                                    val hold2xJob = scope.launch {
                                        if (!currentGestureSettings.masterEnabled || !currentGestureSettings.holdTo2xEnabled) return@launch
                                        delay(380L)
                                        if (!hasDragged && isPlaying) {
                                            isHolding2x = true
                                            try {
                                                exoPlayer?.playbackParameters = PlaybackParameters(2.0f)
                                            } catch (_: Exception) {}
                                        }
                                    }

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressedPointers = event.changes.filter { it.pressed }
                                        if (pressedPointers.size >= 2) {
                                            hold2xJob.cancel()
                                            if (isHolding2x) {
                                                try {
                                                    exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
                                                } catch (_: Exception) {}
                                                isHolding2x = false
                                            }
                                            if (isInteractingVolume) isInteractingVolume = false
                                            if (isInteractingBrightness) isInteractingBrightness = false
                                            if (isSwipeSeeking) {
                                                isSwipeSeeking = false
                                                swipeSeekDeltaMs = 0L
                                            }
                                            dragMode = 5
                                            hasDragged = true

                                            val p1 = pressedPointers[0].position
                                            val p2 = pressedPointers[1].position
                                            val span = kotlin.math.hypot(p1.x - p2.x, p1.y - p2.y)
                                            val centroidY = (p1.y + p2.y) / 2f

                                            if (initialPinchSpan <= 0f) {
                                                initialPinchSpan = span
                                                twoFingerStartY = centroidY
                                            } else if (!pinchTriggered && currentGestureSettings.masterEnabled && currentGestureSettings.pinchToZoomEnabled) {
                                                val spanRatio = if (initialPinchSpan > 0f) span / initialPinchSpan else 1f
                                                val dragYDelta = twoFingerStartY - centroidY

                                                if (spanRatio > 1.18f || dragYDelta > 50.dp.toPx()) {
                                                    pinchTriggered = true
                                                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                    val nextMode = when (resizeMode) {
                                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                                        else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                    }
                                                    notifyFitChange(nextMode)
                                                } else if (spanRatio < 0.82f || dragYDelta < -50.dp.toPx()) {
                                                    pinchTriggered = true
                                                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                    val nextMode = when (resizeMode) {
                                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                    }
                                                    notifyFitChange(nextMode)
                                                }
                                            }
                                            event.changes.forEach { try { it.consume() } catch (_: Exception) {} }
                                            continue
                                        }

                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) {
                                            break
                                        }
                                        val deltaX = change.position.x - startX
                                        val deltaY = startY - change.position.y
                                        val deltaYDown = change.position.y - startY
                                        lastDragYDown = deltaYDown

                                        if (!isFullscreen && onCollapseToMiniPlayer != null && dragMode == 0 && !hasDragged && deltaYDown > touchSlopPx && deltaYDown > kotlin.math.abs(deltaX) * 1.3f) {
                                            dragMode = 4
                                            hasDragged = true
                                            showControls = false
                                            hold2xJob.cancel()
                                            if (isHolding2x) {
                                                try {
                                                    exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
                                                } catch (_: Exception) {}
                                                isHolding2x = false
                                            }
                                        }

                                        if (dragMode == 4) {
                                            try { change.consume() } catch (_: Exception) {}
                                            val currentRootY = exoSurfaceCoordinates?.localToRoot(change.position)?.y ?: change.position.y
                                            val currentDragY = (currentRootY - rootStartY).coerceAtLeast(0f)
                                            lastDragYDown = currentDragY
                                            onDragDownDelta?.invoke(currentDragY)
                                        } else {
                                            if (!hasDragged && (kotlin.math.abs(deltaX) > touchSlopPx || kotlin.math.abs(deltaY) > touchSlopPx)) {
                                            hasDragged = true
                                            hold2xJob.cancel()
                                            if (isHolding2x) {
                                                try {
                                                    exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
                                                } catch (_: Exception) {}
                                                isHolding2x = false
                                            }
                                            val isVolZone = startX > size.width * 0.60f
                                            val isBrightZone = startX < size.width * 0.40f
                                            val g = currentGestureSettings
                                            val isMaster = g.masterEnabled
                                            dragMode = when {
                                                isVolZone -> if (isMaster && g.volumeEnabled) 2 else 0
                                                isBrightZone -> if (isMaster && g.brightnessEnabled) 3 else 0
                                                else -> {
                                                    // Center zone: only dominant horizontal swipe triggers seek
                                                    if (kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) * 1.3f) {
                                                        if (isMaster && g.swipeSeekEnabled) 1 else 0
                                                    } else if (startX > size.width / 2f) {
                                                        if (isMaster && g.volumeEnabled) 2 else 0
                                                    } else {
                                                        if (isMaster && g.brightnessEnabled) 3 else 0
                                                    }
                                                }
                                            }
                                            if (dragMode == 2) {
                                                isInteractingVolume = true
                                                isSwipeSeeking = false
                                                swipeSeekDeltaMs = 0L
                                            } else if (dragMode == 3) {
                                                isInteractingBrightness = true
                                                isSwipeSeeking = false
                                                swipeSeekDeltaMs = 0L
                                            }
                                        }

                                        if (hasDragged) {
                                            try { change.consume() } catch (_: Exception) {}
                                            when (dragMode) {
                                                1 -> {
                                                    // Horizontal Seek
                                                    val dur = durationMs.coerceAtLeast(0L)
                                                    val maxSwipeMs = if (dur > 0L) minOf(180_000L, dur / 3L).coerceAtLeast(60_000L) else 90_000L
                                                    val frac = (deltaX / size.width.toFloat()).coerceIn(-1f, 1f)
                                                    val deltaMs = (frac * maxSwipeMs).toLong()
                                                    val targetMs = if (dur > 0L) {
                                                        (initialSeekPosMs + deltaMs).coerceIn(0L, dur)
                                                    } else {
                                                        (initialSeekPosMs + deltaMs).coerceAtLeast(0L)
                                                    }
                                                    swipeSeekDeltaMs = deltaMs
                                                    swipeSeekTargetMs = targetMs
                                                    isSwipeSeeking = true
                                                }
                                                2 -> {
                                                    // Right side drag: adjusts Volume (Volume HUD appears on Left)
                                                    isSwipeSeeking = false
                                                    isInteractingVolume = true
                                                    val dragFraction = deltaY / (size.height * 0.6f)
                                                    val prevVol = currentVolumeFraction
                                                    val newVol = (startVol + dragFraction).coerceIn(0f, 1f)
                                                    if ((prevVol < 1f && newVol >= 1f) || (prevVol > 0f && newVol <= 0f)) {
                                                        com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                    }
                                                    PlayerGesturePlugin.setVolumeFraction(context, newVol)
                                                    currentVolumeFraction = newVol
                                                    showVolumeHud = true
                                                    showBrightnessHud = false
                                                    hudResetTrigger++
                                                }
                                                3 -> {
                                                    // Left side drag: adjusts Brightness (Brightness HUD appears on Right)
                                                    isSwipeSeeking = false
                                                    isInteractingBrightness = true
                                                    val dragFraction = deltaY / (size.height * 0.6f)
                                                    val newBright = (startBright + dragFraction).coerceIn(0.01f, 1f)
                                                    PlayerGesturePlugin.setBrightnessFraction(activity, newBright)
                                                    currentBrightnessFraction = newBright
                                                    showBrightnessHud = true
                                                    showVolumeHud = false
                                                    hudResetTrigger++
                                                }
                                            }
                                        }
                                    }
                                }

                                    if (dragMode == 5 || pinchTriggered) {
                                        return@awaitEachGesture
                                    }

                                    if (dragMode == 4) {
                                        val finalDragY = lastDragYDown.coerceAtLeast(0f)
                                        if (onDragDownRelease != null) {
                                            onDragDownRelease.invoke(finalDragY)
                                        } else if (finalDragY > 70.dp.toPx()) {
                                            onCollapseToMiniPlayer?.invoke()
                                        }
                                        return@awaitEachGesture
                                    }

                                    isInteractingVolume = false
                                    isInteractingBrightness = false
                                    hold2xJob.cancel()
                                    if (isHolding2x) {
                                        try {
                                            exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
                                        } catch (_: Exception) {}
                                        isHolding2x = false
                                        return@awaitEachGesture
                                    }

                                    if (dragMode == 1 && isSwipeSeeking) {
                                        try {
                                            PlayerPlaybackPlugin.seekTo(exoPlayer, swipeSeekTargetMs)
                                            currentPositionMs = swipeSeekTargetMs
                                        } catch (_: Exception) {}
                                        isSwipeSeeking = false
                                        controlsTimeoutKey++
                                        return@awaitEachGesture
                                    }

                                    if (!hasDragged) {
                                        val now = System.currentTimeMillis()
                                        val isConsecutive = (now - lastTapTime < 340L) && (kotlin.math.abs(startX - lastTapX) < 140f)

                                        if (isConsecutive) {
                                            val g = currentGestureSettings
                                            val doubleTapSeekAllowed = g.masterEnabled && g.doubleTapSeekEnabled

                                            if ((isRightSide || isLeftSide) && doubleTapSeekAllowed) {
                                                singleTapJob?.cancel()
                                                singleTapJob = null
                                                lastTapTime = now
                                                lastTapX = startX

                                                exoPlayer?.let { player ->
                                                    try {
                                                        if (isRightSide) {
                                                            PlayerPlaybackPlugin.seekBy(player, 10000L)
                                                            if (doubleTapSide == "RIGHT") {
                                                                accumulatedSeekSeconds += 10
                                                            } else {
                                                                accumulatedSeekSeconds = 10
                                                            }
                                                            doubleTapSide = "RIGHT"
                                                        } else {
                                                            PlayerPlaybackPlugin.seekBy(player, -10000L)
                                                            if (doubleTapSide == "LEFT") {
                                                                accumulatedSeekSeconds += 10
                                                            } else {
                                                                accumulatedSeekSeconds = 10
                                                            }
                                                            doubleTapSide = "LEFT"
                                                        }
                                                        doubleTapTriggerKey++
                                                        controlsTimeoutKey++
                                                        val curKey = doubleTapTriggerKey
                                                        scope.launch {
                                                            delay(1200)
                                                            if (doubleTapTriggerKey == curKey) {
                                                                doubleTapSide = null
                                                                accumulatedSeekSeconds = 0
                                                                controlsTimeoutKey++
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                            } else if (!isRightSide && !isLeftSide) {
                                                // Center double-tap: Toggle Play / Pause video
                                                singleTapJob?.cancel()
                                                singleTapJob = null
                                                lastTapTime = now
                                                lastTapX = startX

                                                controlsTimeoutKey++
                                                PlayerPlaybackPlugin.togglePlayPause(exoPlayer, context)
                                            } else {
                                                // Double-tap seek disabled on sides: treat as normal single tap
                                                lastTapTime = now
                                                lastTapX = startX
                                                singleTapJob?.cancel()
                                                singleTapJob = scope.launch {
                                                    delay(340L)
                                                    isAutoHideTransition = false
                                                    showControls = !showControls
                                                    controlsTimeoutKey++
                                                }
                                            }
                                        } else {
                                            lastTapTime = now
                                            lastTapX = startX
                                            singleTapJob?.cancel()
                                            singleTapJob = scope.launch {
                                                delay(340L)
                                                isAutoHideTransition = false
                                                showControls = !showControls
                                                controlsTimeoutKey++
                                            }
                                        }
                                    }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (_: Exception) {}
                            }
                        }
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = (if (isMirrored) -1f else 1f) * (videoZoomFactor / 100f)
                                scaleY = videoZoomFactor / 100f
                                colorFilter = ColorFilter.colorMatrix(videoColorMatrix)
                            }
                            .onSizeChanged { glassBackdrop.hostWidth = it.width }
                            .then(if (useGlassBlur) Modifier.hazeSource(hazeState) else Modifier)
                            .then(if (com.styropyr0.prismal.PrismalGlass.isGlassmorphismEnabled) Modifier.prismalGlassLayer(playerPrismalBackdrop) else Modifier),
                        factory = { ctx ->
                            (LayoutInflater.from(ctx).inflate(com.nyyrox.aniverse.R.layout.cine_player_texture, null, false) as PlayerView).apply {
                                useController = false
                                this.resizeMode = currentResizeMode
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                findViewById<View>(androidx.media3.ui.R.id.exo_shutter)?.visibility = View.GONE
                                player = exoPlayer
                                subtitleView?.visibility = View.GONE
                                subtitleView?.alpha = 0f
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            if (view.player !== exoPlayer) {
                                view.player = exoPlayer
                            }
                            view.resizeMode = currentResizeMode
                            // Hide basic Android SubtitleView whenever player UI caption is shown
                            if (isCaptionsEnabled && activeCuesText != null) {
                                view.subtitleView?.visibility = View.GONE
                                view.subtitleView?.alpha = 0f
                                view.subtitleView?.setCues(emptyList())
                            } else {
                                view.subtitleView?.visibility = View.GONE
                                view.subtitleView?.alpha = 0f
                            }
                        }
                    )


                    val volumeInteractingScale by animateFloatAsState(
                        targetValue = if (isInteractingVolume) 1.12f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "volumeInteractingScale"
                    )
                    val brightnessInteractingScale by animateFloatAsState(
                        targetValue = if (isInteractingBrightness) 1.12f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "brightnessInteractingScale"
                    )

                    // Volume toast — glass pill, top-center, appears on volume gesture with interactive zoom
                    AnimatedVisibility(
                        visible = showVolumeHud && !isInPip,
                        enter = fadeIn(tween(120)) + slideInVertically(tween(160, easing = FastOutSlowInEasing)) { -it / 2 },
                        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 2 },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                            .scale(volumeInteractingScale)
                    ) {
                        CinePillIndicator(
                            fraction = currentVolumeFraction,
                            icon = when {
                                currentVolumeFraction <= 0.01f -> Icons.Default.VolumeOff
                                currentVolumeFraction < 0.5f -> Icons.Default.VolumeDown
                                else -> Icons.Default.VolumeUp
                            },
                            label = "Volume"
                        )
                    }

                    // Brightness toast — glass pill, top-center, appears on brightness gesture with interactive zoom
                    AnimatedVisibility(
                        visible = showBrightnessHud && !isInPip,
                        enter = fadeIn(tween(120)) + slideInVertically(tween(160, easing = FastOutSlowInEasing)) { -it / 2 },
                        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 2 },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                            .scale(brightnessInteractingScale)
                    ) {
                        CinePillIndicator(
                            fraction = currentBrightnessFraction,
                            icon = Icons.Default.BrightnessMedium,
                            label = "Brightness"
                        )
                    }

                    // UP NEXT POPUP (Triggers 1 minute before episode ends)
                    val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0L)
                    val isNearEnd = durationMs > 0L && remainingMs in 1000L..60000L && currentPositionMs > 15_000L

                    // Smooth animated progress timeline across 60 seconds
                    val targetProgress = if (durationMs > 0L) (1f - (remainingMs.toFloat() / 60000f)).coerceIn(0f, 1f) else 0f
                    val smoothProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
                        label = "UpNextSmoothProgress"
                    )
                    val timeLeftSeconds = (remainingMs / 1000L).coerceIn(0L, 60L)

                    AnimatedVisibility(
                        visible = isNearEnd && hasNextEpisode && !isUpNextCanceled && !isInPip,
                        enter = fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                                slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.92f, animationSpec = tween(400, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(250)) +
                               slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(250)) +
                               scaleOut(targetScale = 0.95f, animationSpec = tween(250)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = if (showControls) 120.dp else 40.dp, end = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .width(340.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xF010131E))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(OttPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (autoPlayNextEnabled) "UP NEXT IN ${timeLeftSeconds}s" else "UP NEXT",
                                        color = OttSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                androidx.compose.material3.IconButton(
                                    onClick = { isUpNextCanceled = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (nextEpisodeImage != null) {
                                    AsyncImage(
                                        model = nextEpisodeImage,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(86.dp, 54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E2230)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Episode ${episode + 1}",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = nextEpisodeTitle ?: "Next Episode",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (autoPlayNextEnabled) {
                                LinearProgressIndicator(
                                    progress = smoothProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = OttPrimary,
                                    trackColor = Color(0x33FFFFFF)
                                )

                                LaunchedEffect(remainingMs, isPlaying, isUpNextCanceled, isNextTriggered) {
                                    if (!isNextTriggered && !isUpNextCanceled && isPlaying && currentPositionMs > 15_000L && remainingMs in 1L..1000L) {
                                        isNextTriggered = true
                                        onNextEpisode()
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { isUpNextCanceled = true },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF222635),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Stay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        if (!isNextTriggered) {
                                            isNextTriggered = true
                                            onNextEpisode()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OttPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    val btnText = if (autoPlayNextEnabled) "Play Now • ${timeLeftSeconds}s" else "Play Now"
                                    Text(btnText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }

                    // Double-tap Seek Wave Overlay (Right: Forward)
                    AnimatedVisibility(
                        visible = doubleTapSide == "RIGHT" && !isInPip,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        DoubleTapSeekOverlay(
                            isForward = true,
                            accumulatedSeconds = accumulatedSeekSeconds,
                            triggerKey = doubleTapTriggerKey,
                            transitionStyle = playerCustomization.themePack.seekTransitionStyle
                        )
                    }

                    // Double-tap Seek Wave Overlay (Left: Rewind)
                    AnimatedVisibility(
                        visible = doubleTapSide == "LEFT" && !isInPip,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        DoubleTapSeekOverlay(
                            isForward = false,
                            accumulatedSeconds = accumulatedSeekSeconds,
                            triggerKey = doubleTapTriggerKey,
                            transitionStyle = playerCustomization.themePack.seekTransitionStyle
                        )
                    }

                    // 2X Speed Hold Indicator Badge (Appears at TopCenter)
                    AnimatedVisibility(
                        visible = isHolding2x && !isInPip,
                        enter = fadeIn(tween(140)) + scaleIn(tween(140)),
                        exit = fadeOut(tween(180)) + scaleOut(tween(180)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 28.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "2X Speed",
                                tint = OttPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "2X Speed",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                style = LocalTextStyle.current.copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }

                    // Swipe Seek Floating HUD (Appears at Center while dragging)
                    AnimatedVisibility(
                        visible = isSwipeSeeking && !isInPip,
                        enter = fadeIn(tween(100)) + scaleIn(tween(100)),
                        exit = fadeOut(tween(180)) + scaleOut(tween(180)),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 22.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (swipeSeekDeltaMs >= 0) Icons.Default.Forward10 else Icons.Default.Replay10,
                                    contentDescription = null,
                                    tint = OttPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${if (swipeSeekDeltaMs >= 0) "+" else ""}${swipeSeekDeltaMs / 1000}s",
                                    color = OttPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = LocalTextStyle.current.copy(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.85f),
                                            offset = Offset(1f, 1f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                            }
                            Text(
                                text = "${formatTime(swipeSeekTargetMs)} / ${formatTime(durationMs)}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                style = LocalTextStyle.current.copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }

                    // Active Subtitle / Caption Overlay
                    if (isCaptionsEnabled && activeCuesText != null) {
                        val bgCol = if (isInPip) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = captionConfig.bgAlpha)
                        val displayText = if (translationTargetLangCode != null) (translatedCueText ?: activeCuesText!!) else activeCuesText!!
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (isInPip) 6.dp else if (showControls) 88.dp else 36.dp)
                                .padding(horizontal = if (isInPip) 8.dp else 24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgCol)
                                .padding(horizontal = if (isInPip) 6.dp else 12.dp, vertical = if (isInPip) 2.dp else 6.dp)
                        ) {
                            if (captionConfig.hasStroke && captionConfig.strokeWidth > 0f) {
                                Text(
                                    text = displayText,
                                    fontSize = if (isInPip) (captionConfig.fontSizeSp * 0.75f).sp else captionConfig.fontSizeSp.sp,
                                    fontWeight = if (captionConfig.isBold) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    style = LocalTextStyle.current.copy(
                                        color = captionConfig.strokeColor,
                                        drawStyle = Stroke(
                                            width = if (isInPip) (captionConfig.strokeWidth * 0.75f) else captionConfig.strokeWidth,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                )
                            }
                            Text(
                                text = displayText,
                                color = captionConfig.textColor.copy(alpha = captionConfig.textAlpha),
                                fontSize = if (isInPip) (captionConfig.fontSizeSp * 0.75f).sp else captionConfig.fontSizeSp.sp,
                                fontWeight = if (captionConfig.isBold) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                style = if (captionConfig.hasShadow) {
                                    LocalTextStyle.current.copy(
                                        shadow = Shadow(
                                            color = Color.Black,
                                            offset = Offset(2f, 2f),
                                            blurRadius = 4f
                                        )
                                    )
                                } else LocalTextStyle.current
                            )
                        }
                    }

                    // AniSkip Interactive Intro/Outro Skip Button
                    AnimatedVisibility(
                        visible = activeSkipInterval != null && !isInPip,
                        enter = fadeIn() + slideInHorizontally { it / 2 },
                        exit = fadeOut() + slideOutHorizontally { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = if (showControls) 86.dp else 24.dp, end = 16.dp)
                    ) {
                        activeSkipInterval?.let { interval ->
                            val label = when (interval.skipType.lowercase(Locale.US)) {
                                "op", "mixed-op" -> "Skip Intro"
                                "ed", "mixed-ed" -> "Skip Outro"
                                "recap" -> "Skip Recap"
                                else -> "Skip"
                            }

                            GlassPill(
                                onClick = {
                                    PlayerPlaybackPlugin.seekTo(exoPlayer, interval.endTimeMs + 200L)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FastForward,
                                        contentDescription = label,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Floating Sleek Unobtrusive Lock Button (Draggable & Customizable)
                    AnimatedVisibility(
                        visible = isScreenLocked && !isInPip,
                        enter = fadeIn(tween(180)) + scaleIn(tween(180)),
                        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val xPos = (maxWidth * (lockXFraction + playerCustomization.hudLayout.lockDeltaX)).coerceIn(8.dp, (maxWidth - 52.dp).coerceAtLeast(8.dp))
                            val yPos = (maxHeight * (lockYFraction + playerCustomization.hudLayout.lockDeltaY)).coerceIn(8.dp, (maxHeight - 52.dp).coerceAtLeast(8.dp))

                            Box(
                                modifier = Modifier
                                    .offset(x = xPos, y = yPos)
                                    .scale(playerCustomization.hudLayout.lockScale)
                                    .size(44.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                val newPlacement = playerCustomization.placement.copy(
                                                    lockAnchor = LockScreenAnchor.CUSTOM_FREEHAND,
                                                    lockCustomXFraction = lockXFraction,
                                                    lockCustomYFraction = lockYFraction
                                                )
                                                val updated = playerCustomization.copy(placement = newPlacement)
                                                playerCustomization = updated
                                                storageManager.saveCinePlayerCustomization(updated)
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            val newX = lockXFraction + (dragAmount.x / size.width.toFloat())
                                            val newY = lockYFraction + (dragAmount.y / size.height.toFloat())
                                            lockXFraction = newX.coerceIn(0.02f, 0.94f)
                                            lockYFraction = newY.coerceIn(0.02f, 0.94f)
                                        }
                                    }
                                    .clip(CircleShape)
                                    .background(Color(0x80000000))
                                    .clickable {
                                        isScreenLocked = false
                                        showControls = true
                                        controlsTimeoutKey++
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (playerCustomization.icons.lockStyle) {
                                        LockIconStyle.SLEEK_PADLOCK -> Icons.Default.Lock
                                        LockIconStyle.HEX_KEY -> Icons.Default.Lock
                                        LockIconStyle.TECH_SHIELD -> Icons.Default.CheckCircle
                                        LockIconStyle.MINIMAL_RING -> Icons.Default.PlayCircle
                                    },
                                    contentDescription = "Tap to Unlock (Drag to Move)",
                                    tint = themePalette.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Resumed playback indicator (Clean Transparent Background)
                    if (!isInPip) {
                        resumeToastText?.let { rText ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 24.dp)
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = themePalette.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = rText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = LocalTextStyle.current.copy(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.85f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Aspect ratio / Fit Mode change notification HUD (Clean Transparent Background)
                    if (!isInPip) {
                        fitNotificationText?.let { fText ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 24.dp)
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = themePalette.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = fText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = LocalTextStyle.current.copy(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.85f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Playback Error Details Card
                    if (!isInPip) {
                        playbackErrorCode?.let { errCode ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xF20D101C))
                                    .border(
                                        1.dp,
                                        Color(0x35FFFFFF),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 20.dp, vertical = 18.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.widthIn(max = 360.dp)
                                ) {
                                    // Error icon
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x20FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Text(
                                        text = "Playback Failed",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    // Error code badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x25FFFFFF))
                                            .border(0.5.dp, Color(0x35FFFFFF), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = errCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFF8A80),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Issue details
                                    playbackErrorInfo?.let { info ->
                                        Text(
                                            text = info,
                                            fontSize = 12.sp,
                                            color = Color(0xFFD0D4DC),
                                            textAlign = TextAlign.Center,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    // Action Controls: Retry & Dismiss
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Retry Button
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(themePalette.primary)
                                                .clickable {
                                                    playbackErrorCode = null
                                                    playbackErrorInfo = null
                                                    exoPlayer?.prepare()
                                                    exoPlayer?.play()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }

                                        // Direct Web Player Fallback Button
                                        if (!isDirectStream) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0x2210B981))
                                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        playbackErrorCode = null
                                                        playbackErrorInfo = null
                                                        engineMode = PlayerEngineMode.IFRAME_FALLBACK
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Web Player", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        // Dismiss Button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    playbackErrorCode = null
                                                    playbackErrorInfo = null
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                         ) {
                                             Text("Dismiss", fontSize = 12.sp, color = Color.White)
                                         }
                                    }
                                }
                            }
                        }
                    }

                    // Buffering Spinner
                    if ((isBuffering || !hasStartedPlayback) && !isInPip) {
                        if (!hasStartedPlayback) {
                            CineStreamLoader(
                                modifier = Modifier.align(Alignment.Center),
                                accentColor = playerCustomization.accentColor.color,
                                subtitle = if (isDirectStream) "Direct · HD" else null
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(42.dp),
                                color = playerCustomization.accentColor.color,
                                trackColor = Color.White.copy(alpha = 0.18f),
                                strokeWidth = 3.2.dp
                            )
                        }
                    }

                    // Web CinePlayer Overlay (.zp-sc/.zp-top/.zp-mid/.zp-bot)
                    var zpLastVol by remember { mutableFloatStateOf(0.5f) }

                    // Floating AniSkip Button (.zp-sk)
                    val currentSkipInterval = skipIntervals.find { currentPositionMs in it.startTimeMs..it.endTimeMs }
                    if (currentSkipInterval != null && !isInPip) {
                        val isIntro = currentSkipInterval.skipType.contains("op", ignoreCase = true) || currentSkipInterval.skipType.contains("intro", ignoreCase = true)
                        ZpAniSkipBtn(
                            label = if (isIntro) "Skip Opening" else "Skip Ending",
                            onClick = {
                                PlayerPlaybackPlugin.seekTo(exoPlayer, currentSkipInterval.endTimeMs)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = if (showControls) 86.dp else 24.dp)
                        )
                    }

                    // Floating Episode Welcome Card (.zp-card)
                    if (showEpisodeWelcomeCard && !showControls && !isInPip) {
                        ZpEpisodeCard(
                            title = title,
                            episode = episode,
                            episodeTitle = episodeTitle,
                            isDub = isDub,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }

                    // Floating Hold-to-Speed Badge (.zp-hold)
                    if (isHolding2x && !isInPip) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color(0xCC000000))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(99.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FastForward, null, tint = ZpRed, modifier = Modifier.size(16.dp))
                                Text(
                                    text = (if (holdSpeedMultiplier % 1f == 0f) holdSpeedMultiplier.toInt().toString() else holdSpeedMultiplier.toString()) + "× Speed",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showControls && !isScreenLocked && !isInPip,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val u = if (glassCompact) 9.dp else 12.dp
                        val tap = if (glassCompact) 32.dp else 38.dp
                        val ping = { showControls = true; isAutoHideTransition = false; controlsTimeoutKey++ }
                        val openTab = { tab: PlayerSettingsTab, a: Rect ->
                            morphAnchor = a; activeSettingsTab = tab; showPlayerSettingsModal = true; controlsTimeoutKey++
                        }
                        val isCaptionActive = isCaptionsEnabled && (activeCuesText != null || subtitleTracks.any { it.isSelected })

                        Box(Modifier.fillMaxSize()) {
                            // .zp-sc scrim
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.55f),
                                        0.22f to Color.Transparent,
                                        0.66f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )

                            // .zp-top
                            Row(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(start = u * 1.6f, end = u * 1.6f, top = u * 1.2f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(u)
                            ) {
                                ZpBtn(tap, onClick = { onBack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(tap * 0.52f))
                                }

                                Column(Modifier.weight(1f)) {
                                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "Episode $episode" + (if (!episodeTitle.isNullOrBlank()) " · $episodeTitle" else "") + (if (currentIsDub) " · DUB" else " · SUB"),
                                        color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(u * 0.7f), verticalAlignment = Alignment.CenterVertically) {
                                    if (audioTracks.size > 1) {
                                        ZpBtn(tap, isPill = true, onClick = { openTab(PlayerSettingsTab.AUDIO, it) }) {
                                            Icon(Icons.Default.Translate, "Audio track", tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                audioTracks.firstOrNull { it.isSelected }?.label ?: "Audio",
                                                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    } else {
                                        ZpBtn(tap, isPill = true, onClick = {
                                            currentIsDub = !currentIsDub
                                            val targetLang = if (currentIsDub) "en" else "ja"
                                            audioTracks.firstOrNull { it.language.startsWith(targetLang, ignoreCase = true) || (if (currentIsDub) it.label.contains("Dub", true) else it.label.contains("Sub", true)) }?.let { tr ->
                                                exoPlayer?.let { p ->
                                                    try {
                                                        val group = p.currentTracks.groups[tr.groupIndex]
                                                        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, tr.trackIndex))
                                                            .build()
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        }) {
                                            Text(
                                                if (isDub) "DUB" else "SUB",
                                                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (availableServers.isNotEmpty()) {
                                        val activeSrv = availableServers.find { it.streamUrl == streamUrl } ?: availableServers.firstOrNull()
                                        ZpBtn(tap, isPill = true, onClick = { openTab(PlayerSettingsTab.SERVER, it) }) {
                                            Icon(Icons.Default.Dns, "Servers", tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                (activeSrv?.serverName ?: "Server") + " HD",
                                                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                                            )
                                        }
                                    }

                                    ZpBtn(tap, onClick = { openTab(PlayerSettingsTab.MAIN, it) }) {
                                        Icon(Icons.Outlined.Settings, "Settings", tint = Color.White, modifier = Modifier.size(tap * 0.52f))
                                    }

                                    ZpBtn(tap, onClick = { onToggleFullscreen() }) {
                                        Icon(
                                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            if (isFullscreen) "Exit fullscreen" else "Fullscreen", tint = Color.White, modifier = Modifier.size(tap * 0.52f)
                                        )
                                    }
                                }
                            }

                            // .zp-mid
                            Row(
                                Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(u * 1.8f)
                            ) {
                                ZpBtn(u * 3.4f, enabled = hasPreviousEpisode, onClick = { ping(); onPreviousEpisode() }) {
                                    Icon(Icons.Default.SkipPrevious, "Previous episode", tint = Color.White, modifier = Modifier.size(u * 3.4f * 0.52f))
                                }
                                ZpSeekBtn(isForward = false, seconds = skipStepSeconds, size = u * 4f, onClick = {
                                    ping()
                                    PlayerPlaybackPlugin.seekBy(exoPlayer, -skipStepSeconds * 1000L)
                                })
                                ZpHeroBtn(
                                    isPlaying = isPlaying,
                                    size = minOf(u * 6f, 76.dp),
                                    onClick = {
                                        ping()
                                        PlayerPlaybackPlugin.togglePlayPause(exoPlayer, context)
                                    }
                                )
                                ZpSeekBtn(isForward = true, seconds = skipStepSeconds, size = u * 4f, onClick = {
                                    ping()
                                    PlayerPlaybackPlugin.seekBy(exoPlayer, skipStepSeconds * 1000L)
                                })
                                ZpBtn(u * 3.4f, enabled = hasNextEpisode, onClick = { ping(); onNextEpisode() }) {
                                    Icon(Icons.Default.SkipNext, "Next episode", tint = Color.White, modifier = Modifier.size(u * 3.4f * 0.52f))
                                }
                            }

                            // .zp-bot
                            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = u * 1.6f, end = u * 1.6f, bottom = u * 0.6f)) {
                                // .zp-sr: time | seek | duration
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u)) {
                                    val activePosMs = if (isScrubbing) scrubPositionMs else currentPositionMs
                                    Text(
                                        formatTime(activePosMs),
                                        color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 40.dp)
                                    )
                                    BoxWithConstraints(Modifier.weight(1f).height(28.dp), contentAlignment = Alignment.CenterStart) {
                                        val w = maxWidth
                                        val playedFrac = if (durationMs > 0L) (activePosMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                                        val bufferedFrac = if (durationMs > 0L) (bufferedPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

                                        // .zp-tr
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(if (isScrubbing) 5.dp else 3.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.18f))
                                        ) {
                                            Box(Modifier.fillMaxHeight().fillMaxWidth(bufferedFrac).background(Color.White.copy(alpha = 0.28f)))
                                            Box(Modifier.fillMaxHeight().fillMaxWidth(playedFrac).background(ZpRed))
                                            if (durationMs > 0L) {
                                                skipIntervals.forEach { iv ->
                                                    val s = (iv.startTimeMs.toFloat() / durationMs).coerceIn(0f, 1f)
                                                    val e = (iv.endTimeMs.toFloat() / durationMs).coerceIn(0f, 1f)
                                                    if (e > s) {
                                                        val isIntro = iv.skipType.contains("op", ignoreCase = true) || iv.skipType.contains("intro", ignoreCase = true)
                                                        val segColor = if (isIntro) Color(0x9900E5FF) else Color(0x99FF9100)
                                                        Box(Modifier.offset(x = w * s).width(w * (e - s)).fillMaxHeight().background(segColor))
                                                    }
                                                }
                                            }
                                        }
                                        // .zp-th
                                        Box(
                                            Modifier
                                                .offset(x = (w * playedFrac - 6.dp).coerceAtLeast(0.dp))
                                                .size(if (isScrubbing) 14.dp else 10.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .border(2.dp, ZpRed, CircleShape)
                                        )
                                        // .zp-tip
                                        if (isScrubbing && durationMs > 0L) {
                                            Text(
                                                text = formatTime(scrubPositionMs),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .offset(x = (w * playedFrac - 24.dp).coerceAtLeast(0.dp), y = (-26).dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xD9000000))
                                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Slider(
                                            value = playedFrac,
                                            onValueChange = { f ->
                                                controlsTimeoutKey++
                                                isScrubbing = true
                                                scrubPositionMs = (f * durationMs).toLong().coerceIn(0L, durationMs)
                                            },
                                            onValueChangeFinished = {
                                                isScrubbing = false
                                                currentPositionMs = scrubPositionMs
                                                PlayerPlaybackPlugin.seekTo(exoPlayer, scrubPositionMs)
                                                controlsTimeoutKey++
                                            },
                                            colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    val remainingMs = (durationMs - activePosMs).coerceAtLeast(0L)
                                    Text(
                                        text = if (showRemainingTime) "-${formatTime(remainingMs)}" else formatTime(durationMs),
                                        color = Color.White.copy(alpha = 0.78f),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.widthIn(min = 44.dp).clickable { showRemainingTime = !showRemainingTime }
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                // .zp-cr: volume | spacer | speed, quality, CC, settings, PiP, fullscreen
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u * 0.4f)) {
                                    ZpBtn(tap, onClick = {
                                        ping()
                                        val v = if (currentVolumeFraction > 0f) { zpLastVol = currentVolumeFraction; 0f } else zpLastVol
                                        PlayerGesturePlugin.setVolumeFraction(context, v)
                                        currentVolumeFraction = v
                                    }) {
                                        Icon(
                                            if (currentVolumeFraction == 0f) Icons.AutoMirrored.Filled.VolumeOff else if (currentVolumeFraction < 0.5f) Icons.AutoMirrored.Filled.VolumeDown else Icons.AutoMirrored.Filled.VolumeUp,
                                            if (currentVolumeFraction == 0f) "Unmute" else "Mute", tint = Color.White, modifier = Modifier.size(tap * 0.52f)
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    ZpBtn(tap, isPill = true, onClick = { openTab(PlayerSettingsTab.SPEED, it) }) {
                                        Text(
                                            (if (playbackSpeed % 1f == 0f) playbackSpeed.toInt().toString() else playbackSpeed.toString()) + "×",
                                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (qualityTracks.isNotEmpty()) {
                                        ZpBtn(tap, isPill = true, onClick = { openTab(PlayerSettingsTab.QUALITY, it) }) {
                                            Text(
                                                qualityTracks.firstOrNull { it.isSelected }?.label ?: "Auto",
                                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    ZpBtn(tap, isSelected = isCaptionActive, onClick = { morphAnchor = it; showCaptionModal = true; controlsTimeoutKey++ }) {
                                        Text("CC", color = if (isCaptionActive) ZpRed else Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    ZpBtn(tap, onClick = { openTab(PlayerSettingsTab.MAIN, it) }) {
                                        Icon(Icons.Outlined.Settings, "Settings", tint = Color.White, modifier = Modifier.size(tap * 0.52f))
                                    }
                                    ZpBtn(tap, onClick = { (context as? ComponentActivity)?.let { MainActivity.enterPip(it) } }) {
                                        Icon(Icons.Outlined.PictureInPictureAlt, "Picture in picture", tint = Color.White, modifier = Modifier.size(tap * 0.52f))
                                    }
                                    ZpBtn(tap, onClick = { onToggleFullscreen() }) {
                                        Icon(
                                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            if (isFullscreen) "Exit fullscreen" else "Fullscreen", tint = Color.White, modifier = Modifier.size(tap * 0.52f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // YouTube-style bottom border loading line
                    AnimatedVisibility(
                        visible = (isBuffering || (isInitialLoading && !hasStartedPlayback)) && !isInPip,
                        enter = fadeIn(tween(140)),
                        exit = fadeOut(tween(140)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        YtBottomLoadingBar(accentColor = playerCustomization.accentColor.color)
                    }
                }
                }
            }

            PlayerEngineMode.IFRAME_FALLBACK -> {
                IframePlayerView(
                    embedUrl = streamUrl,
                    modifier = Modifier.fillMaxSize(),
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = onToggleFullscreen
                )
            }
        }

    }
    }

    // Consolidated Playback Settings Modal
    if (showPlayerSettingsModal) {
        PlayerSettingsModal(
            activeTab = activeSettingsTab,
            onTabChange = { tab: PlayerSettingsTab -> activeSettingsTab = tab },
            onDismiss = {
                showPlayerSettingsModal = false
                activeSettingsTab = PlayerSettingsTab.MAIN
            },
            anchor = morphAnchor,
            onOpenCaptions = {
                showPlayerSettingsModal = false
                activeSettingsTab = PlayerSettingsTab.MAIN
                showCaptionModal = true
            },
            currentCustomization = playerCustomization,
            onApplyCustomization = { newSettings ->
                playerCustomization = newSettings
                storageManager.saveCinePlayerCustomization(newSettings)
            },
            qualityTracks = qualityTracks,
            onSelectQuality = { track: QualityTrackInfo ->
                preferredQualityHeight = track.height
                exoPlayer?.let { p ->
                    val group = p.currentTracks.groups[track.groupIndex]
                    p.trackSelectionParameters = p.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                        )
                        .build()
                }
            },
            playbackSpeed = playbackSpeed,
            onSelectSpeed = { spd: Float ->
                playbackSpeed = spd
                exoPlayer?.playbackParameters = PlaybackParameters(spd)
            },
            audioTracks = audioTracks,
            onSelectAudio = { track: AudioTrackInfo ->
                val chosenLang = if (track.language.isNotBlank() && track.language != "und") track.language else track.label
                preferredAudioLanguage = chosenLang
                try {
                    scope.launch(Dispatchers.IO) {
                        UserPreferences.saveAudioLanguage(context, chosenLang)
                    }
                } catch (_: Exception) {}
                exoPlayer?.let { p ->
                    try {
                        val group = p.currentTracks.groups[track.groupIndex]
                        val lang = if (track.language.isNotBlank() && track.language != "und") track.language else null
                        p.trackSelectionParameters = p.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setOverrideForType(
                                TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                            )
                            .setPreferredAudioLanguage(lang)
                            .build()
                    } catch (_: Exception) {}
                }
            },
            autoPlayNextEnabled = autoPlayNextEnabled,
            onToggleAutoPlayNext = { checked: Boolean ->
                autoPlayNextEnabled = checked
                storageManager.setAutoPlayNext(checked)
                onToggleAutoPlayNext(checked)
            },
            autoSkipIntroOutroEnabled = autoSkipIntroOutroEnabled,
            onToggleAutoSkipIntroOutro = { checked: Boolean ->
                autoSkipIntroOutroEnabled = checked
                storageManager.setAutoSkipIntroOutro(checked)
                onToggleAutoSkipIntroOutro(checked)
            },
            skipFillerEnabled = skipFillerEnabled,
            onToggleSkipFiller = { checked: Boolean ->
                skipFillerEnabled = checked
            },
            availableServers = availableServers,
            currentStreamUrl = streamUrl,
            onSelectServer = { srv ->
                onSelectServer?.invoke(srv)
            },
            isDub = currentIsDub,
            onToggleDub = { dub ->
                currentIsDub = dub
                val targetLang = if (currentIsDub) "en" else "ja"
                audioTracks.firstOrNull { it.language.startsWith(targetLang, ignoreCase = true) || (if (currentIsDub) it.label.contains("Dub", true) else it.label.contains("Sub", true)) }?.let { tr ->
                    exoPlayer?.let { p ->
                        try {
                            val group = p.currentTracks.groups[tr.groupIndex]
                            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, tr.trackIndex))
                                .build()
                        } catch (_: Exception) {}
                    }
                }
            },
            videoAspectRatio = videoAspectRatio,
            onSelectAspectRatio = { videoAspectRatio = it },
            videoZoomFactor = videoZoomFactor,
            onZoomChange = { videoZoomFactor = it },
            isMirrored = isMirrored,
            onToggleMirror = { isMirrored = it },
            isUpscalerEnabled = isUpscalerEnabled,
            onToggleUpscaler = { isUpscalerEnabled = it },
            upscalerMode = upscalerMode,
            onSelectUpscalerMode = { upscalerMode = it },
            upscalerIntensity = upscalerIntensity,
            onUpscalerIntensityChange = { upscalerIntensity = it },
            videoFilterPreset = videoFilterPreset,
            onSelectFilterPreset = { videoFilterPreset = it },
            videoBrightness = videoBrightness,
            onBrightnessChange = { videoBrightness = it },
            videoContrast = videoContrast,
            onContrastChange = { videoContrast = it },
            videoSaturation = videoSaturation,
            onSaturationChange = { videoSaturation = it },
            onResetPicture = {
                videoFilterPreset = "off"
                videoBrightness = 100
                videoContrast = 100
                videoSaturation = 100
            },
            skipStepSeconds = skipStepSeconds,
            onSelectSkipStep = { skipStepSeconds = it },
            holdSpeedMultiplier = holdSpeedMultiplier,
            onSelectHoldRate = { holdSpeedMultiplier = it },
            autoHideTimeoutMs = autoHideTimeoutMs,
            onSelectAutoHide = { autoHideTimeoutMs = it },
            onResetPlayback = {
                skipStepSeconds = 10
                holdSpeedMultiplier = 2.0f
                autoHideTimeoutMs = 3200L
            }
        )
    }

    // Subtitle Customization Modal (Responsive Tabs & Dual-Pane Layout)
    if (showCaptionModal) {
        var modalTab by remember { mutableIntStateOf(0) }
        var showSearch by remember { mutableStateOf(false) }

        GlassDialog(
            anchor = morphAnchor,
            centered = true,
            onDismiss = { showCaptionModal = false }
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 560.dp)
                    .fillMaxHeight(0.86f)
                    .glassPanel(28.dp)
            ) {
                val isNarrow = maxWidth < 620.dp

                // Subtitle Search & Retrieval logic
                LaunchedEffect(selectedSubSeason, selectedSubEpisode, showCaptionModal) {
                    if (showCaptionModal) {
                        isSearchingCaptions = true
                        captionSearchMessage = null
                        try {
                            val queryTitle = captionSearchQuery.ifBlank { title }
                            val directSubs = try {
                                SubtitleRepository.fetchAllSubtitles(
                                    anilistId = anilistId,
                                    title = queryTitle,
                                    episode = selectedSubEpisode,
                                    season = selectedSubSeason,
                                    audio = if (isDub) "dub" else "sub"
                                )
                            } catch (_: Exception) {
                                emptyList()
                            }
                            val searchSubs = if (directSubs.isEmpty()) {
                                try {
                                    SubtitleRepository.searchSubtitlesByTitle(
                                        title = queryTitle,
                                        season = selectedSubSeason,
                                        episode = selectedSubEpisode
                                    )
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            } else emptyList()

                            searchedSubtitles = (directSubs + searchSubs).distinctBy { it.url }
                            captionSearchMessage = if (searchedSubtitles.isNotEmpty()) {
                                "Found ${searchedSubtitles.size} captions for S${selectedSubSeason}:E${selectedSubEpisode}"
                            } else {
                                "No subtitles found for S${selectedSubSeason}:E${selectedSubEpisode}"
                            }
                        } catch (_: Exception) {
                            searchedSubtitles = emptyList()
                            captionSearchMessage = "Unable to fetch subtitles"
                        } finally {
                            isSearchingCaptions = false
                        }
                    }
                }

                val availableExternal = remember(externalSubtitles, searchedSubtitles, selectedSubEpisode, episode) {
                    if (searchedSubtitles.isNotEmpty()) {
                        searchedSubtitles
                    } else if (selectedSubEpisode == episode) {
                        externalSubtitles
                    } else {
                        searchedSubtitles
                    }
                }

                val availableLanguages = remember(availableExternal) {
                    val set = linkedSetOf("All")
                    availableExternal.forEach { s ->
                        if (s.language.isNotBlank()) set.add(s.language)
                    }
                    set.toList()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isNarrow) 12.dp else 16.dp)
                ) {
                    // Header: title + actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = GlassAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Subtitles", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlassPill(onClick = { uploadSubLauncher.launch("*/*") }) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Upload", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            GlassPill(onClick = { showCaptionModal = false; showDialoguePanel = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                    Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Dialogue", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(
                                onClick = { showCaptionModal = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // 3-part segmented tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(3.dp)
                    ) {
                        listOf("Captions", "Style", "Sync").forEachIndexed { i, label ->
                            val sel = modalTab == i
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (sel) Color.White else Color.Transparent)
                                    .clickable { modalTab = i }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (sel) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Reusable Captions List Content
                    val captionsContent: @Composable () -> Unit = {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Current caption + turn off
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Now showing", color = Color(0x99FFFFFF), fontSize = 10.5.sp)
                                    Text(
                                        text = if (isCaptionsEnabled && activeCaptionTitle != "Off") activeCaptionTitle else "Off",
                                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    if (captionPlayStatus == CaptionPlayStatus.ERROR && captionErrorText != null) {
                                        Text(captionErrorText ?: "Error loading subtitle", color = Color(0xFFFF6B6B), fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (isCaptionsEnabled && (activeCaptionTitle != "Off" || activeLoadedCues.isNotEmpty())) {
                                    GlassPill(onClick = {
                                        isCaptionsEnabled = false
                                        activeLoadedCues = emptyList()
                                        activeLoadedSubtitleUrl = null
                                        activeCuesText = null
                                        captionPlayStatus = CaptionPlayStatus.OFF
                                        activeCaptionTitle = "Off"
                                        exoPlayer?.let { p ->
                                            p.trackSelectionParameters = p.trackSelectionParameters
                                                .buildUpon()
                                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                                .build()
                                        }
                                    }) {
                                        Text("Turn off", color = Color(0xFFFF6B6B), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                GlassPill(onClick = { showSearch = !showSearch }) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Find more", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (showSearch) {
                            // Season & Episode Quick Picker Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Season Stepper
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "SEASON",
                                        color = OttSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selectedSubSeason > 1) Color(0x2EFFFFFF) else Color(0x10FFFFFF))
                                            .clickable(enabled = selectedSubSeason > 1) {
                                                selectedSubSeason -= 1
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "−",
                                            color = if (selectedSubSeason > 1) Color.White else Color(0x40FFFFFF),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(GlassAccent.copy(alpha = 0.21f))
                                            .border(1.dp, GlassAccent.copy(alpha = 0.50f), OttPillShape)
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "S$selectedSubSeason",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x2EFFFFFF))
                                            .clickable {
                                                selectedSubSeason += 1
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Separator
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(1.dp)
                                        .background(Color(0x20FFFFFF))
                                )

                                // Episode Stepper
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "EPISODE",
                                        color = OttSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selectedSubEpisode > 1) Color(0x2EFFFFFF) else Color(0x10FFFFFF))
                                            .clickable(enabled = selectedSubEpisode > 1) {
                                                selectedSubEpisode -= 1
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "−",
                                            color = if (selectedSubEpisode > 1) Color.White else Color(0x40FFFFFF),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(GlassAccent.copy(alpha = 0.21f))
                                            .border(1.dp, GlassAccent.copy(alpha = 0.50f), OttPillShape)
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "EP $selectedSubEpisode",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x2EFFFFFF))
                                            .clickable {
                                                selectedSubEpisode += 1
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Show Name Search Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = OttTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = captionSearchQuery,
                                    onValueChange = { captionSearchQuery = it },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(GlassAccent),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (captionSearchQuery.isNotBlank()) {
                                                isSearchingCaptions = true
                                                captionSearchMessage = null
                                                scope.launch {
                                                    try {
                                                        val res = SubtitleRepository.searchSubtitlesByTitle(
                                                            title = captionSearchQuery,
                                                            season = selectedSubSeason,
                                                            episode = selectedSubEpisode
                                                        )
                                                        searchedSubtitles = res
                                                        captionSearchMessage = if (res.isNotEmpty()) {
                                                            "Found ${res.size} captions"
                                                        } else {
                                                            "No subtitles found"
                                                        }
                                                    } catch (_: Exception) {
                                                        captionSearchMessage = "Error searching subtitles"
                                                    } finally {
                                                        isSearchingCaptions = false
                                                    }
                                                }
                                            }
                                        }
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (captionSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Search show name...",
                                                color = OttTextMuted,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                if (captionSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { captionSearchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = OttTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Search Action Button (High Contrast Red)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GlassAccent)
                                        .clickable {
                                            if (captionSearchQuery.isNotBlank()) {
                                                isSearchingCaptions = true
                                                captionSearchMessage = null
                                                scope.launch {
                                                    try {
                                                        val res = SubtitleRepository.searchSubtitlesByTitle(
                                                            title = captionSearchQuery,
                                                            season = selectedSubSeason,
                                                            episode = selectedSubEpisode
                                                        )
                                                        searchedSubtitles = res
                                                        captionSearchMessage = if (res.isNotEmpty()) {
                                                            "Found ${res.size} captions"
                                                        } else {
                                                            "No subtitles found"
                                                        }
                                                    } catch (_: Exception) {
                                                        captionSearchMessage = "Error searching subtitles"
                                                    } finally {
                                                        isSearchingCaptions = false
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Search",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            }

                            if (isSearchingCaptions) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = GlassAccent,
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Searching OpenSubtitles...",
                                        color = OttTextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else if (captionSearchMessage != null) {
                                Text(
                                    text = captionSearchMessage ?: "",
                                    color = if (searchedSubtitles.isNotEmpty()) Color(0xFF30D158) else OttTextMuted,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            var selectedSourceFilter by remember { mutableStateOf("All") }
                            var selectedLanguageFilter by remember { mutableStateOf("All") }

                            val hasStreamSubs = remember(availableExternal) {
                                availableExternal.any { it.source.startsWith("Stream", ignoreCase = true) || it.source.contains("Direct", ignoreCase = true) || it.source.contains("KAA", ignoreCase = true) || it.source.contains("Reanime", ignoreCase = true) }
                            }
                            val hasOpenSubs = remember(availableExternal) {
                                availableExternal.any { it.source.contains("OpenSubtitles", ignoreCase = true) || it.source.contains("Stremio", ignoreCase = true) }
                            }

                            // Dynamic Language Filter Chips
                            if (availableLanguages.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    availableLanguages.forEach { lang ->
                                        val isSel = selectedLanguageFilter.equals(lang, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (isSel) Color.White else Color(0x16FFFFFF))
                                                .border(0.5.dp, if (isSel) Color.White else Color(0x22FFFFFF), RoundedCornerShape(999.dp))
                                                .clickable { selectedLanguageFilter = lang }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = lang,
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            // Scrollable Subtitles List
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filteredExternal = availableExternal.filter { sub ->
                                    val matchesLang = selectedLanguageFilter.equals("All", ignoreCase = true) || sub.language.equals(selectedLanguageFilter, ignoreCase = true)
                                    val matchesSource = when (selectedSourceFilter) {
                                        "Stream" -> sub.source.startsWith("Stream", ignoreCase = true) || sub.source.contains("Direct", ignoreCase = true) || sub.source.contains("KAA", ignoreCase = true) || sub.source.contains("Reanime", ignoreCase = true)
                                        "OpenSubtitles" -> sub.source.contains("OpenSubtitles", ignoreCase = true) || sub.source.contains("Stremio", ignoreCase = true)
                                        else -> true
                                    }
                                    matchesLang && matchesSource
                                }.sortedByDescending { sub ->
                                    if (sub.source.startsWith("Stream", ignoreCase = true) || sub.source.contains("Direct", ignoreCase = true)) 2 else 1
                                }

                                Text(
                                    text = "Available (${filteredExternal.size})",
                                    color = OttTextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                if (filteredExternal.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No subtitles matching filters.\nType a show name above to search OpenSubtitles.",
                                            color = OttTextMuted,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    filteredExternal.forEach { sub ->
                                        val isDownloadingThis = downloadingSubtitleId == sub.id
                                        val isCurrent = isCaptionsEnabled && (activeLoadedSubtitleUrl == sub.url || (activeLoadedSubtitleUrl == null && activeCaptionTitle == sub.label)) && captionPlayStatus == CaptionPlayStatus.PLAYING

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isCurrent) Color(0x2810B981) else Color(0x14FFFFFF))
                                                .border(
                                                    1.dp,
                                                    if (isCurrent) Color(0xFF30D158) else Color(0x22FFFFFF),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable(enabled = !isDownloadingThis) {
                                                    val previousTitle = activeCaptionTitle
                                                    val previousUrl = activeLoadedSubtitleUrl
                                                    captionPlayStatus = CaptionPlayStatus.DOWNLOADING
                                                    downloadingSubtitleId = sub.id
                                                    captionErrorText = null
                                                    preferredSubLanguage = sub.language

                                                    scope.launch {
                                                        try {
                                                            val dlResult = SubtitleRepository.downloadSubtitleFile(context, sub)
                                                            dlResult.onSuccess { file ->
                                                                try {
                                                                    val raw = file.readText(Charsets.UTF_8)
                                                                    val cues = SubtitleParser.parse(raw)
                                                                    if (cues.isNotEmpty()) {
                                                                        activeLoadedCues = cues
                                                                        activeLoadedSubtitleUrl = sub.url
                                                                        isCaptionsEnabled = true
                                                                        activeCaptionTitle = sub.label
                                                                        captionPlayStatus = CaptionPlayStatus.PLAYING
                                                                        captionErrorText = null
                                                                        downloadingSubtitleId = null

                                                                        val pos = (currentPositionMs - subSyncMs + 120L).coerceAtLeast(0L)
                                                                        val activeCues = findActiveCuesAt(cues, pos)
                                                                        activeCuesText = if (activeCues.isNotEmpty()) activeCues.map { it.text }.distinct().joinToString("\n") else null
                                                                    } else {
                                                                        captionPlayStatus = CaptionPlayStatus.ERROR
                                                                        captionErrorText = "No dialogue cues found in ${sub.label}"
                                                                        activeCaptionTitle = previousTitle
                                                                        activeLoadedSubtitleUrl = previousUrl
                                                                        downloadingSubtitleId = null
                                                                    }
                                                                } catch (e: Exception) {
                                                                    captionPlayStatus = CaptionPlayStatus.ERROR
                                                                    captionErrorText = "Failed to parse subtitle cues"
                                                                    activeCaptionTitle = previousTitle
                                                                    activeLoadedSubtitleUrl = previousUrl
                                                                    downloadingSubtitleId = null
                                                                }
                                                            }.onFailure { err ->
                                                                captionPlayStatus = CaptionPlayStatus.ERROR
                                                                captionErrorText = err.message ?: "Failed to download subtitle"
                                                                activeCaptionTitle = previousTitle
                                                                activeLoadedSubtitleUrl = previousUrl
                                                                downloadingSubtitleId = null
                                                            }
                                                        } catch (e: Exception) {
                                                            captionPlayStatus = CaptionPlayStatus.ERROR
                                                            captionErrorText = e.message ?: "Failed to download subtitle"
                                                            activeCaptionTitle = previousTitle
                                                            activeLoadedSubtitleUrl = previousUrl
                                                            downloadingSubtitleId = null
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(GlassAccent.copy(alpha = 0.21f))
                                                            .border(0.5.dp, GlassAccent.copy(alpha = 0.38f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                    ) {
                                                        Text(
                                                            text = sub.language.uppercase(),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0x22FFFFFF))
                                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                    ) {
                                                        Text(
                                                            text = sub.source,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFFCAD5E2),
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = sub.label,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    softWrap = false
                                                )
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            when {
                                                isDownloadingThis -> {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                                isCurrent -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(OttPillShape)
                                                            .background(Color(0x3010B981))
                                                            .border(0.8.dp, Color(0xFF30D158), OttPillShape)
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = "Active",
                                                                tint = Color(0xFF30D158),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Text(
                                                                text = "PLAYING",
                                                                color = Color(0xFF30D158),
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(OttPillShape)
                                                            .background(Color(0x20FFFFFF))
                                                            .border(0.8.dp, Color(0x35FFFFFF), OttPillShape)
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Download,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Text(
                                                                text = "Select",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    val styleContent: @Composable () -> Unit = {
                        Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Live Preview Box
                            Text(
                                text = "Preview",
                                color = Color(0xCCFFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x33000000))
                                    .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = captionConfig.bgAlpha))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    if (captionConfig.hasStroke && captionConfig.strokeWidth > 0f) {
                                        Text(
                                            text = "Aniverse Subtitle Preview",
                                            fontSize = captionConfig.fontSizeSp.sp,
                                            fontWeight = if (captionConfig.isBold) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            softWrap = false,
                                            style = LocalTextStyle.current.copy(
                                                color = captionConfig.strokeColor,
                                                drawStyle = Stroke(
                                                    width = captionConfig.strokeWidth,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        )
                                    }
                                    Text(
                                        text = "Aniverse Subtitle Preview",
                                        color = captionConfig.textColor.copy(alpha = captionConfig.textAlpha),
                                        fontSize = captionConfig.fontSizeSp.sp,
                                        fontWeight = if (captionConfig.isBold) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = if (captionConfig.hasShadow) {
                                            LocalTextStyle.current.copy(
                                                shadow = Shadow(
                                                    color = Color.Black,
                                                    offset = Offset(2f, 2f),
                                                    blurRadius = 4f
                                                )
                                            )
                                        } else LocalTextStyle.current
                                    )
                                }
                            }

                            GlassSliderRow(
                                label = "Size",
                                valueText = "${(((captionConfig.fontSizeSp - 10f) / 30f) * 100f).roundToInt().coerceIn(0, 100)}",
                                value = ((captionConfig.fontSizeSp - 10f) / 30f) * 100f,
                                onChange = { captionConfig = captionConfig.copy(fontSizeSp = 10f + it / 100f * 30f) }
                            )
                            GlassSliderRow(
                                label = "Background",
                                valueText = "${(captionConfig.bgAlpha * 100f).roundToInt()}",
                                value = captionConfig.bgAlpha * 100f,
                                onChange = { captionConfig = captionConfig.copy(bgAlpha = it / 100f) }
                            )
                            GlassSliderRow(
                                label = "Outline",
                                valueText = "${((captionConfig.strokeWidth / 8f) * 100f).roundToInt().coerceIn(0, 100)}",
                                value = (captionConfig.strokeWidth / 8f) * 100f,
                                onChange = {
                                    val w = it / 100f * 8f
                                    captionConfig = captionConfig.copy(strokeWidth = w, hasStroke = w > 0.05f)
                                }
                            )
                            Text(
                                text = "Text color",
                                color = Color(0xCCFFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFFFEB3B),
                                    Color(0xFF00E5FF),
                                    Color(0xFF69F0AE),
                                    Color(0xFFFF80AB)
                                ).forEach { col ->
                                    val isSel = captionConfig.textColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (isSel) 2.5.dp else 1.dp,
                                                color = if (isSel) GlassAccent else Color(0x40FFFFFF),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                captionConfig = captionConfig.copy(textColor = col)
                                            }
                                    )
                                }
                            }

                            Text(
                                text = "Outline color",
                                color = Color(0xCCFFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    Color(0xFF000000),
                                    Color(0xFF262626),
                                    Color(0xFF4A4A4A),
                                    Color(0xFF8B0000),
                                    Color(0xFF0D1B2A),
                                    Color(0xFFFFFFFF)
                                ).forEach { col ->
                                    val isSel = captionConfig.strokeColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (isSel) 2.5.dp else 1.dp,
                                                color = if (isSel) GlassAccent else Color(0x40FFFFFF),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                captionConfig = captionConfig.copy(strokeColor = col)
                                            }
                                    )
                                }
                            }

                        }
                    }

                    val syncContent: @Composable () -> Unit = {
                        Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassSliderRow(
                                label = "Subtitle delay",
                                valueText = (if (subSyncMs > 0) "+" else "") + String.format(Locale.US, "%.2fs", subSyncMs / 1000.0),
                                value = subSyncMs / 1000f,
                                range = -10f..10f,
                                onChange = { subSyncMs = (it * 4f).roundToInt() * 250L }
                            )
                            if (subSyncMs != 0L) {
                                Text(
                                    "Reset",
                                    color = GlassAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { subSyncMs = 0L }.padding(vertical = 2.dp)
                                )
                            }
                            // Real-time Subtitle Translation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = GlassAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Live translation",
                                        color = Color(0xCCFFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                                if (translationTargetLangCode != null) {
                                    val langName = TranslationService.SUPPORTED_LANGUAGES.entries.firstOrNull { it.value == translationTargetLangCode }?.key ?: translationTargetLangCode.orEmpty()
                                    Text(
                                        text = langName,
                                        color = Color(0xFF30D158),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isOff = translationTargetLangCode == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isOff) Color.White else Color(0x14FFFFFF))
                                        .border(0.5.dp, if (isOff) Color.White else Color(0x22FFFFFF), RoundedCornerShape(999.dp))
                                        .clickable { translationTargetLangCode = null }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Off",
                                        color = if (isOff) Color.Black else Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isOff) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                TranslationService.SUPPORTED_LANGUAGES.forEach { (name, code) ->
                                    val isSelected = translationTargetLangCode == code
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isSelected) GlassAccent else Color(0x14FFFFFF))
                                            .border(0.5.dp, if (isSelected) GlassAccent else Color(0x22FFFFFF), RoundedCornerShape(999.dp))
                                            .clickable { translationTargetLangCode = code }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        when (modalTab) {
                            0 -> captionsContent()
                            1 -> styleContent()
                            else -> syncContent()
                        }
                    }
                }
            }
        }
    }
    // Real-time Dialogue & Transcript Panel Modal
    if (showDialoguePanel) {
        DialoguePanelModal(
            cues = activeLoadedCues,
            currentPositionMs = currentPositionMs,
            subSyncMs = subSyncMs,
            onSubSyncChange = { newOffset -> subSyncMs = newOffset },
            isFullscreen = isFullscreen,
            onSeekTo = { posMs ->
                PlayerPlaybackPlugin.seekTo(exoPlayer, posMs)
            },
            onClose = { showDialoguePanel = false },
            onUploadClick = { uploadSubLauncher.launch("*/*") },
            translationTargetLangCode = translationTargetLangCode,
            translationTargetLangName = translationTargetLangName,
            onSelectTranslation = { code, name ->
                translationTargetLangCode = code
                translationTargetLangName = name
            },
            isTranslating = isTranslating
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Composable
private fun DialoguePanelModal(
    cues: List<SubtitleCue>,
    currentPositionMs: Long,
    subSyncMs: Long,
    onSubSyncChange: (Long) -> Unit,
    isFullscreen: Boolean,
    onSeekTo: (Long) -> Unit,
    onClose: () -> Unit,
    onUploadClick: () -> Unit,
    translationTargetLangCode: String?,
    translationTargetLangName: String?,
    onSelectTranslation: (code: String?, name: String?) -> Unit,
    isTranslating: Boolean,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val adjustedPos = (currentPositionMs - subSyncMs).coerceAtLeast(0L)
    val activeCueIndex = remember(cues, adjustedPos) {
        findCueIndexAt(cues, adjustedPos)
    }

    // Auto-scroll to keep active cue centered
    LaunchedEffect(activeCueIndex) {
        if (activeCueIndex >= 0 && searchQuery.isBlank() && !listState.isScrollInProgress) {
            val targetScroll = (activeCueIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    val displayCues = remember(cues, searchQuery) {
        if (searchQuery.isBlank()) {
            cues.mapIndexed { idx, cue -> idx to cue }
        } else {
            cues.mapIndexedNotNull { idx, cue ->
                if (cue.text.contains(searchQuery, ignoreCase = true)) idx to cue else null
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isFullscreen) Alignment.CenterEnd else Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .then(
                        if (isFullscreen) {
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.48f)
                                .widthIn(max = 440.dp)
                        } else {
                            Modifier
                                .fillMaxWidth(0.95f)
                                .fillMaxHeight(0.88f)
                        }
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0D0F14).copy(alpha = 0.96f))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = OttPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Dialogue Transcript",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${cues.size} lines • Live Sync",
                                    color = OttTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Upload button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x18FFFFFF))
                                    .clickable { onUploadClick() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.UploadFile,
                                    contentDescription = "Upload",
                                    tint = OttPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Upload",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Close button
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x20FFFFFF))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Search input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = OttTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search dialogue...",
                                        color = OttTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        color = Color.White,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = OttTextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }

                    // Translation bar in Dialogue panel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Translate:",
                            color = OttSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val isOff = translationTargetLangCode == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isOff) OttPrimary else Color(0x16FFFFFF))
                                .clickable { onSelectTranslation(null, null) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Off",
                                color = if (isOff) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TranslationService.SUPPORTED_LANGUAGES.forEach { (name, code) ->
                            val isSel = translationTargetLangCode == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFF10B981) else Color(0x16FFFFFF))
                                    .clickable { onSelectTranslation(code, name) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Dialogue cues list
                    if (cues.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No subtitles loaded yet",
                                    color = OttTextMuted,
                                    fontSize = 12.sp
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(OttPrimary)
                                        .clickable { onUploadClick() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Upload .SRT / .VTT",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else if (displayCues.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No dialogue matches \"$searchQuery\"",
                                color = OttTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                    // Live Timing Sync Bar with +/- Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val syncSec = subSyncMs / 1000.0
                        val syncText = (if (subSyncMs > 0) "+" else "") + String.format(Locale.US, "%.1fs", syncSec)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Timing Sync:",
                                color = OttSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = syncText,
                                color = if (subSyncMs != 0L) OttPrimary else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x22FFFFFF))
                                    .clickable { onSubSyncChange(subSyncMs - 500L) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("-0.5s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x22FFFFFF))
                                    .clickable { onSubSyncChange(subSyncMs + 500L) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+0.5s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            if (subSyncMs != 0L) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x22E50914))
                                        .clickable { onSubSyncChange(0L) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Reset", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(displayCues, key = { _, pair -> "${pair.first}_${pair.second.startMs}" }) { _, (originalIndex, cue) ->
                            val isActive = originalIndex == activeCueIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) Color(0x33E50914) else Color(0x10FFFFFF))
                                    .border(
                                        width = if (isActive) 1.5.dp else 0.5.dp,
                                        color = if (isActive) Color(0xFFE50914) else Color(0x16FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onSeekTo(cue.startMs)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Timestamp pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isActive) Color(0xFFE50914) else Color(0x22FFFFFF))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = formatTime(cue.startMs),
                                        color = if (isActive) Color.White else OttTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Dialogue line (translated if translation active)
                                val dialogueText = if (translationTargetLangCode != null) {
                                    TranslationService.getCached(cue.text, translationTargetLangCode) ?: cue.text
                                } else {
                                    cue.text
                                }
                                Text(
                                    text = dialogueText,
                                    color = if (isActive) Color.White else Color(0xDDFFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )

                                // Sync this dialogue line to current video time
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isActive) Color(0x44E50914) else Color(0x18FFFFFF))
                                        .border(0.5.dp, if (isActive) Color(0xFFE50914) else Color(0x25FFFFFF), RoundedCornerShape(6.dp))
                                        .clickable {
                                            val diff = currentPositionMs - cue.startMs
                                            onSubSyncChange(diff)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Sync",
                                            tint = if (isActive) Color.White else OttPrimary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "Sync",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerSettingsModal(
    activeTab: PlayerSettingsTab,
    onTabChange: (PlayerSettingsTab) -> Unit,
    onOpenCaptions: () -> Unit = {},
    anchor: androidx.compose.ui.geometry.Rect? = null,
    onDismiss: () -> Unit,
    onApplyCustomization: (CinePlayerCustomizationSettings) -> Unit = {},
    currentCustomization: CinePlayerCustomizationSettings = CinePlayerCustomizationSettings(),
    qualityTracks: List<QualityTrackInfo>,
    onSelectQuality: (QualityTrackInfo) -> Unit,
    playbackSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    audioTracks: List<AudioTrackInfo>,
    onSelectAudio: (AudioTrackInfo) -> Unit,
    autoPlayNextEnabled: Boolean,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    autoSkipIntroOutroEnabled: Boolean,
    onToggleAutoSkipIntroOutro: (Boolean) -> Unit,
    skipFillerEnabled: Boolean = false,
    onToggleSkipFiller: (Boolean) -> Unit = {},
    availableServers: List<StreamServerOption> = emptyList(),
    currentStreamUrl: String = "",
    onSelectServer: (StreamServerOption) -> Unit = {},
    isDub: Boolean = false,
    onToggleDub: (Boolean) -> Unit = {},
    videoAspectRatio: String = "Fit",
    onSelectAspectRatio: (String) -> Unit = {},
    videoZoomFactor: Float = 100f,
    onZoomChange: (Float) -> Unit = {},
    isMirrored: Boolean = false,
    onToggleMirror: (Boolean) -> Unit = {},
    isUpscalerEnabled: Boolean = false,
    onToggleUpscaler: (Boolean) -> Unit = {},
    upscalerMode: String = "modeA",
    onSelectUpscalerMode: (String) -> Unit = {},
    upscalerIntensity: Int = 80,
    onUpscalerIntensityChange: (Int) -> Unit = {},
    videoFilterPreset: String = "off",
    onSelectFilterPreset: (String) -> Unit = {},
    videoBrightness: Int = 100,
    onBrightnessChange: (Int) -> Unit = {},
    videoContrast: Int = 100,
    onContrastChange: (Int) -> Unit = {},
    videoSaturation: Int = 100,
    onSaturationChange: (Int) -> Unit = {},
    onResetPicture: () -> Unit = {},
    skipStepSeconds: Int = 10,
    onSelectSkipStep: (Int) -> Unit = {},
    holdSpeedMultiplier: Float = 2.0f,
    onSelectHoldRate: (Float) -> Unit = {},
    autoHideTimeoutMs: Long = 3200L,
    onSelectAutoHide: (Long) -> Unit = {},
    onResetPlayback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var mainTabSelection by remember { mutableStateOf("video") }

    GlassDialog(
        anchor = anchor,
        onDismiss = onDismiss
    ) {
        Box(
            modifier = modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF00A0A0A))
                .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            when (activeTab) {
                PlayerSettingsTab.MAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Settings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // .zp-tabs: [ Video ] [ Picture ] [ Playback ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                "video" to "Video",
                                "picture" to "Picture",
                                "play" to "Playback"
                            ).forEach { (k, label) ->
                                val isSelected = mainTabSelection == k
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { mainTabSelection = k }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(2.dp)
                                            .background(if (isSelected) ZpRed else Color.Transparent)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Tab 1: Video
                        if (mainTabSelection == "video") {
                            ZpSec("Aspect ratio")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VIDEO_ASPECTS.forEach { ar ->
                                    ZpPill(
                                        label = ar,
                                        isSelected = videoAspectRatio == ar,
                                        onClick = { onSelectAspectRatio(ar) }
                                    )
                                }
                            }

                            ZpSec("Zoom (${videoZoomFactor.toInt()}%)")
                            Slider(
                                value = videoZoomFactor,
                                onValueChange = onZoomChange,
                                valueRange = 100f..150f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = ZpRed,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth().height(26.dp)
                            )

                            ZpRow(
                                title = "Mirror video",
                                checked = isMirrored,
                                onCheckedChange = onToggleMirror
                            )

                            ZpSec("4K upscaler (GPU)")
                            ZpRow(
                                title = "Anime4K GPU upscaling",
                                checked = isUpscalerEnabled,
                                onCheckedChange = onToggleUpscaler
                            )

                            if (isUpscalerEnabled) {
                                ZpSec("Anime4K Model / Shader Preset")
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ANIME4K_MODES.forEach { m ->
                                        ZpPill(
                                            label = m.label,
                                            isSelected = upscalerMode == m.id,
                                            onClick = { onSelectUpscalerMode(m.id) }
                                        )
                                    }
                                }

                                val currentModeDesc = ANIME4K_MODES.find { it.id == upscalerMode }?.desc ?: ""
                                ZpNote(currentModeDesc)

                                ZpSec("Upscale Intensity ($upscalerIntensity%)")
                                Slider(
                                    value = upscalerIntensity.toFloat(),
                                    onValueChange = { onUpscalerIntensityChange(it.toInt()) },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = ZpRed,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(26.dp)
                                )
                            }

                            ZpNote("Runs in real-time on your GPU with neural shaders. Best on 720p–1080p anime.")
                        }

                        // Tab 2: Picture
                        if (mainTabSelection == "picture") {
                            ZpSec("Visual Enhancements & Shaders")
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VIDEO_FILTER_PRESETS.forEach { p ->
                                    ZpPill(
                                        label = p.label,
                                        isSelected = videoFilterPreset == p.id,
                                        onClick = { onSelectFilterPreset(p.id) }
                                    )
                                }
                            }

                            ZpSec("Brightness ($videoBrightness%)")
                            Slider(
                                value = videoBrightness.toFloat(),
                                onValueChange = { onBrightnessChange(it.toInt()) },
                                valueRange = 70f..130f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = ZpRed,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth().height(26.dp)
                            )

                            ZpSec("Contrast ($videoContrast%)")
                            Slider(
                                value = videoContrast.toFloat(),
                                onValueChange = { onContrastChange(it.toInt()) },
                                valueRange = 70f..140f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = ZpRed,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth().height(26.dp)
                            )

                            ZpSec("Color Saturation ($videoSaturation%)")
                            Slider(
                                value = videoSaturation.toFloat(),
                                onValueChange = { onSaturationChange(it.toInt()) },
                                valueRange = 70f..160f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = ZpRed,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth().height(26.dp)
                            )

                            if (videoFilterPreset != "off" || videoBrightness != 100 || videoContrast != 100 || videoSaturation != 100) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x18FFFFFF))
                                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable { onResetPicture() }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text("Reset visual shaders", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }

                        // Tab 3: Playback
                        if (mainTabSelection == "play") {
                            ZpSec("Automation")
                            ZpRow(
                                title = "Autoplay next episode",
                                checked = autoPlayNextEnabled,
                                onCheckedChange = onToggleAutoPlayNext
                            )
                            ZpRow(
                                title = "Auto-skip intro & outro",
                                checked = autoSkipIntroOutroEnabled,
                                onCheckedChange = onToggleAutoSkipIntroOutro
                            )
                            ZpRow(
                                title = "Skip filler episodes",
                                checked = skipFillerEnabled,
                                onCheckedChange = onToggleSkipFiller
                            )

                            ZpSec("Skip step (buttons, double-tap)")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s").forEach { (s, l) ->
                                    ZpPill(
                                        label = l,
                                        isSelected = skipStepSeconds == s,
                                        onClick = { onSelectSkipStep(s) }
                                    )
                                }
                            }

                            ZpSec("Hold-to-speed rate")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1.5f to "1.5×", 2.0f to "2×", 3.0f to "3×", 4.0f to "4×").forEach { (r, l) ->
                                    ZpPill(
                                        label = l,
                                        isSelected = holdSpeedMultiplier == r,
                                        onClick = { onSelectHoldRate(r) }
                                    )
                                }
                            }

                            ZpSec("Auto-hide controls")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(2000L to "2s", 3200L to "3s", 5000L to "5s", 0L to "Never").forEach { (t, l) ->
                                    ZpPill(
                                        label = l,
                                        isSelected = autoHideTimeoutMs == t,
                                        onClick = { onSelectAutoHide(t) }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                                    .clickable { onResetPlayback() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Reset playback options", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }

                PlayerSettingsTab.SERVER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { onTabChange(PlayerSettingsTab.MAIN) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text("Servers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color(0x1FFFFFFF))
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (!isDub) ZpRed else Color.Transparent)
                                    .clickable { onToggleDub(false) }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("SUB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (isDub) ZpRed else Color.Transparent)
                                    .clickable { onToggleDub(true) }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("DUB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        availableServers.forEach { srv ->
                            val isSelected = srv.streamUrl == currentStreamUrl
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ZpRed.copy(alpha = 0.35f) else Color(0x10FFFFFF))
                                    .border(1.dp, if (isSelected) ZpRed else Color(0x1FFFFFFF), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectServer(srv)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(srv.serverName, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x22FFFFFF))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text("HD", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ZpRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                PlayerSettingsTab.AUDIO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(onClick = { onTabChange(PlayerSettingsTab.MAIN) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Text("Audio Track", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        audioTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (track.isSelected) ZpRed.copy(alpha = 0.35f) else Color(0x10FFFFFF))
                                    .border(1.dp, if (track.isSelected) ZpRed else Color(0x1FFFFFFF), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectAudio(track)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Translate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(track.label, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (track.isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ZpRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                PlayerSettingsTab.QUALITY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(onClick = { onTabChange(PlayerSettingsTab.MAIN) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Text("Quality", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        qualityTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (track.isSelected) ZpRed.copy(alpha = 0.35f) else Color(0x10FFFFFF))
                                    .border(1.dp, if (track.isSelected) ZpRed else Color(0x1FFFFFFF), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectQuality(track)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(track.label, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                if (track.isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ZpRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                PlayerSettingsTab.SPEED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(onClick = { onTabChange(PlayerSettingsTab.MAIN) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Text("Playback Speed", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { spd ->
                            val isSelected = playbackSpeed == spd
                            val label = (if (spd % 1f == 0f) spd.toInt().toString() else spd.toString()) + "×"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ZpRed.copy(alpha = 0.35f) else Color(0x10FFFFFF))
                                    .border(1.dp, if (isSelected) ZpRed else Color(0x1FFFFFFF), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectSpeed(spd)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ZpRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                else -> {
                    LaunchedEffect(Unit) {
                        onTabChange(PlayerSettingsTab.MAIN)
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    masterEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (checked) Color(0x2234C759) else Color(0x15FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) Color(0xFF34C759) else Color(0x80FFFFFF),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = if (masterEnabled) Color.White else Color(0x99FFFFFF),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0x40FFFFFF)
            )
        )
    }
}

@Composable
private fun SettingsItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = Color(0x99FFFFFF),
                        fontSize = 11.sp
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0x80FFFFFF),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SubHeader(
    title: String,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun YtBottomLoadingBar(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ytLoadingSweep")
    val sweepFrac by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepFrac"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val width = size.width
                    val lineLen = width * 0.42f
                    val startX = (width * sweepFrac) - (lineLen / 2f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = 0.65f),
                                accentColor,
                                accentColor.copy(alpha = 0.65f),
                                Color.Transparent
                            ),
                            startX = startX,
                            endX = startX + lineLen
                        )
                    )
                }
        )
    }
}

@Composable
private fun YtBottomProgressBar(
    playedFrac: Float,
    bufferedFrac: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .background(Color.White.copy(alpha = 0.18f))
    ) {
        if (bufferedFrac > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bufferedFrac.coerceIn(0f, 1f))
                    .background(Color.White.copy(alpha = 0.38f))
            )
        }
        if (playedFrac > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(playedFrac.coerceIn(0f, 1f))
                    .background(accentColor)
            )
        }
    }
}

