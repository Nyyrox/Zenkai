package com.nyyrox.aniverse.player

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
import androidx.compose.ui.text.font.FontStyle
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
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
        rawId.contains("jpn") || rawId.contains("ja") || (!rawLabel.isNullOrBlank() && (rawLabel.contains("jap", ignoreCase = true) || rawLabel.contains("native", ignoreCase = true))) -> "ja"
        rawId.contains("eng") || rawId.contains("en") || (!rawLabel.isNullOrBlank() && (rawLabel.contains("eng", ignoreCase = true) || rawLabel.contains("dub", ignoreCase = true))) -> "en"
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
        !rawLabel.isNullOrBlank() && rawLabel.contains("native", ignoreCase = true) -> "Japanese (Native)"
        !rawLabel.isNullOrBlank() && (rawLabel.contains("english-2", ignoreCase = true) || rawLabel.equals("english", ignoreCase = true)) -> "English (Dub)"
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
    val sizeScale: Float = 1.0f,
    val sizePreset: String = "Medium",
    val fontFamilyName: String = "Roboto",
    val textColor: Color = Color.White,
    val bgAlpha: Float = 0.55f,
    val bgColor: Color = Color.Black,
    val hasBgBox: Boolean = true,
    val boxCornerRadiusDp: Float = 6f,
    val textAlpha: Float = 1.0f,
    val hasShadow: Boolean = true,
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val isUppercase: Boolean = false,
    val letterSpacingSp: Float = 0f,
    val weightPreset: String = "Normal",
    val hasStroke: Boolean = true,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 3.0f,
    val activePreset: String = "Default",
    val bottomPaddingDp: Float = 18f
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
    val alphaAnim = remember { Animatable(0f) }
    val offsetAnim = remember { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        alphaAnim.snapTo(0.9f)
        offsetAnim.snapTo(if (isForward) -4f else 4f)
        offsetAnim.animateTo(
            targetValue = if (isForward) 4f else -4f,
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        )
        alphaAnim.animateTo(0.6f, tween(140))
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.40f)
            .clip(
                if (isForward) RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp)
                else RoundedCornerShape(topEnd = 100.dp, bottomEnd = 100.dp)
            )
            .background(
                Brush.horizontalGradient(
                    if (isForward) listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.12f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.offset(x = offsetAnim.value.dp).graphicsLayer { this.alpha = alphaAnim.value }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = if (isForward) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "${if (isForward) "+" else "-"}${accumulatedSeconds}s",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

private enum class PlayerSettingsTab {
    MAIN,
    QUALITY,
    SERVER,
    ORIENTATION,
    SPEED,
    AUDIO,
    THEME_PACKS,
    GESTURES,
    UPSCALER,
    PICTURE
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
    onSelectServer: ((StreamServerOption) -> Unit)? = null,
    serverName: String = "",
    hasDubAvailable: Boolean = true,
    onSwitchAudioStream: ((Boolean) -> Unit)? = null
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
    var videoFilters by remember { mutableStateOf(storageManager.getVideoFilters()) }
    var upscalerSettings by remember { mutableStateOf(storageManager.getUpscalerSettings()) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    LaunchedEffect(videoFilters, upscalerSettings, playerViewRef) {
        playerViewRef?.let { pv ->
            applyVideoEffects(pv, videoFilters, upscalerSettings)
        }
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
    var showCaptionSettingsModal by remember { mutableStateOf(false) }
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

        // 2. Find best candidate from externalSubtitles (prioritizing active server captions) or fetch directly
        val activeServer = serverName.lowercase(Locale.US)
        var candidateTrack = externalSubtitles.firstOrNull {
            (it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)) &&
            activeServer.isNotBlank() && it.source.lowercase(Locale.US).contains(activeServer)
        } ?: externalSubtitles.firstOrNull {
            (it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)) &&
            (it.source.startsWith("Direct") || it.source == "Anikoto" || it.source == "FlixCloud" || it.source == "ACDN" || it.source == "MegaPlay" || it.source == "Vidstream")
        } ?: externalSubtitles.firstOrNull {
            it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)
        } ?: externalSubtitles.firstOrNull {
            (it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)) &&
            activeServer.isNotBlank() && it.source.lowercase(Locale.US).contains(activeServer)
        } ?: externalSubtitles.firstOrNull {
            (it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)) &&
            (it.source.startsWith("Direct") || it.source == "Anikoto" || it.source == "FlixCloud" || it.source == "ACDN" || it.source == "MegaPlay" || it.source == "Vidstream")
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
                    audio = if (isDub) "dub" else "sub",
                    activeServerName = serverName
                )
                candidateTrack = fetched.firstOrNull {
                    (it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)) &&
                    activeServer.isNotBlank() && it.source.lowercase(Locale.US).contains(activeServer)
                } ?: fetched.firstOrNull {
                    (it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)) &&
                    (it.source.startsWith("Direct") || it.source == "Anikoto" || it.source == "FlixCloud" || it.source == "ACDN" || it.source == "MegaPlay" || it.source == "Vidstream")
                } ?: fetched.firstOrNull {
                    it.language.equals(preferredSubLanguage, ignoreCase = true) || it.label.contains(preferredSubLanguage, ignoreCase = true)
                } ?: fetched.firstOrNull {
                    (it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)) &&
                    activeServer.isNotBlank() && it.source.lowercase(Locale.US).contains(activeServer)
                } ?: fetched.firstOrNull {
                    (it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)) &&
                    (it.source.startsWith("Direct") || it.source == "Anikoto" || it.source == "FlixCloud" || it.source == "ACDN" || it.source == "MegaPlay" || it.source == "Vidstream")
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
                var exoSurfaceCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
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
                            .onSizeChanged { glassBackdrop.hostWidth = it.width }
                            .then(if (useGlassBlur) Modifier.hazeSource(hazeState) else Modifier),
                        factory = { ctx ->
                            (LayoutInflater.from(ctx).inflate(com.nyyrox.aniverse.R.layout.cine_player_texture, null, false) as PlayerView).apply {
                                playerViewRef = this
                                useController = false
                                this.resizeMode = resizeMode
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                findViewById<View>(androidx.media3.ui.R.id.exo_shutter)?.visibility = View.GONE
                                player = exoPlayer
                                subtitleView?.visibility = View.GONE
                                subtitleView?.alpha = 0f
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                post {
                                    applyVideoEffects(this, videoFilters, upscalerSettings)
                                }
                            }
                        },
                        update = { view ->
                            playerViewRef = view
                            if (view.player !== exoPlayer) {
                                view.player = exoPlayer
                            }
                            view.resizeMode = resizeMode
                            applyVideoEffects(view, videoFilters, upscalerSettings)
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
                        enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.9f, animationSpec = tween(120)),
                        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.9f, animationSpec = tween(140)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xB3181D28))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "2x",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
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
                        val rawText = if (translationTargetLangCode != null) (translatedCueText ?: activeCuesText!!) else activeCuesText!!
                        val displayText = if (captionConfig.isUppercase) rawText.uppercase() else rawText
                        val fontFam = resolveFontFamily(captionConfig.fontFamilyName)
                        val effectiveSp = if (isInPip) (captionConfig.fontSizeSp * captionConfig.sizeScale * 0.75f).sp else (captionConfig.fontSizeSp * captionConfig.sizeScale).sp
                        val fontWeight = if (captionConfig.isBold) FontWeight.Bold else FontWeight.Normal
                        val fontStyle = if (captionConfig.isItalic) FontStyle.Italic else FontStyle.Normal
                        val letterSpacing = captionConfig.letterSpacingSp.sp

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (isInPip) 6.dp else if (showControls) (captionConfig.bottomPaddingDp + 48f).dp else captionConfig.bottomPaddingDp.dp)
                                .padding(horizontal = if (isInPip) 8.dp else 24.dp)
                                .then(
                                    if (captionConfig.hasBgBox && captionConfig.bgAlpha > 0f) {
                                        Modifier
                                            .clip(RoundedCornerShape(captionConfig.boxCornerRadiusDp.dp))
                                            .background(captionConfig.bgColor.copy(alpha = captionConfig.bgAlpha))
                                            .padding(horizontal = if (isInPip) 6.dp else 12.dp, vertical = if (isInPip) 2.dp else 5.dp)
                                    } else Modifier
                                )
                        ) {
                            if (captionConfig.hasStroke && captionConfig.strokeWidth > 0f) {
                                Text(
                                    text = displayText,
                                    fontFamily = fontFam,
                                    fontSize = effectiveSp,
                                    fontWeight = fontWeight,
                                    fontStyle = fontStyle,
                                    letterSpacing = letterSpacing,
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
                                fontFamily = fontFam,
                                fontSize = effectiveSp,
                                fontWeight = fontWeight,
                                fontStyle = fontStyle,
                                letterSpacing = letterSpacing,
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

                    // 1. iOS-style Gradient Scrim — dark top + dark bottom, transparent center
                    AnimatedVisibility(
                        visible = showControls && !isScreenLocked && !isInPip,
                        enter = fadeIn(animationSpec = tween(140, easing = LinearEasing)),
                        exit = fadeOut(animationSpec = tween(120, easing = LinearEasing)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val overlayOpacity = playerCustomization.glass.overlayOpacity
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = (0.70f * overlayOpacity).coerceIn(0f, 1f)),
                                        0.18f to Color.Black.copy(alpha = (0.38f * overlayOpacity).coerceIn(0f, 1f)),
                                        0.35f to Color.Transparent,
                                        0.68f to Color.Transparent,
                                        0.85f to Color.Black.copy(alpha = (0.38f * overlayOpacity).coerceIn(0f, 1f)),
                                        1.0f to Color.Black.copy(alpha = (0.75f * overlayOpacity).coerceIn(0f, 1f))
                                    )
                                )
                        )
                    }

                    // 2. Top Controls Bar (Arrives first: slides down from top; YouTube fade/slide exit)
                    AnimatedVisibility(
                        visible = showControls && !isScreenLocked && !isInPip,
                        enter = slideInVertically(
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            initialOffsetY = { -it / 2 }
                        ) + fadeIn(tween(140, easing = LinearEasing)),
                        exit = if (isAutoHideTransition) {
                            slideOutVertically(
                                animationSpec = tween(180, easing = FastOutLinearInEasing),
                                targetOffsetY = { -it / 2 }
                            ) + fadeOut(tween(220, easing = LinearEasing))
                        } else {
                            fadeOut(tween(120, easing = LinearEasing))
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (glassCompact) 12.dp else 20.dp, vertical = if (glassCompact) 8.dp else 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Top-left glass pill: back/minimize, settings, PiP, quality, lock
                            GlassPill {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (!isFullscreen && onCollapseToMiniPlayer != null) {
                                        GlassPillIcon(
                                            icon = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Collapse to Mini-player",
                                            onClick = { onCollapseToMiniPlayer() }
                                        )
                                    } else if (isFullscreen) {
                                        GlassPillIcon(
                                            icon = Icons.Default.ArrowBack,
                                            contentDescription = "Exit Fullscreen",
                                            onClick = { onToggleFullscreen() }
                                        )
                                    }
                                    GlassPillIcon(
                                        icon = Icons.Outlined.Settings,
                                        contentDescription = "Settings",
                                        onClick = { morphAnchor = it
                                            activeSettingsTab = PlayerSettingsTab.MAIN
                                            showPlayerSettingsModal = true
                                            controlsTimeoutKey++
                                        }
                                    )
                                    GlassPillIcon(
                                        icon = Icons.Outlined.PictureInPictureAlt,
                                        contentDescription = "Picture in Picture",
                                        onClick = {
                                            (context as? ComponentActivity)?.let { act ->
                                                MainActivity.enterPip(act)
                                            }
                                        }
                                    )
                                    GlassPillIcon(
                                        icon = Icons.Outlined.HighQuality,
                                        contentDescription = "Quality",
                                        onClick = { morphAnchor = it
                                            activeSettingsTab = PlayerSettingsTab.QUALITY
                                            showPlayerSettingsModal = true
                                            controlsTimeoutKey++
                                        }
                                    )
                                    if (upscalerSettings.enabled) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x22FFFFFF))
                                                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    morphAnchor = null
                                                    activeSettingsTab = PlayerSettingsTab.UPSCALER
                                                    showPlayerSettingsModal = true
                                                    controlsTimeoutKey++
                                                }
                                                .padding(horizontal = 7.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "4K",
                                                color = Color.White,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                    GlassPillIcon(
                                        icon = if (isScreenLocked) playerCustomization.themePack.lockIcon else playerCustomization.themePack.unlockIcon,
                                        contentDescription = "Lock Screen",
                                        iconSize = 16.dp,
                                        onClick = {
                                            isScreenLocked = true
                                            showControls = false
                                            showUnlockPrompt = false
                                        }
                                    )
                                }
                            }

                            // Top-right spacer for balance
                            Spacer(Modifier.size(36.dp))
                        }
                    }

                    // 3. Center Controls Deck (YouTube fade/scale exit)
                    AnimatedVisibility(
                        visible = (showControls || isSkipActive) && !isScreenLocked && !isInPip,
                        enter = scaleIn(
                            initialScale = 0.88f,
                            animationSpec = tween(170, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(130, easing = LinearEasing)),
                        exit = if (isAutoHideTransition) {
                            scaleOut(
                                targetScale = 0.92f,
                                animationSpec = tween(160, easing = FastOutLinearInEasing)
                            ) + fadeOut(tween(200, easing = LinearEasing))
                        } else {
                            fadeOut(tween(110, easing = LinearEasing))
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .scale(playerCustomization.placement.centerScale)
                            .offset(y = playerCustomization.placement.centerOffsetY.dp)
                    ) {
                        val centerLayout = playerCustomization.placement.centerLayout
                        val buttonSpacing = if (glassCompact) 2.dp else if (centerLayout == CenterControlsLayout.WIDE_SPACED) 14.dp else 6.dp
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                        ) {
                                if (centerLayout == CenterControlsLayout.STANDARD_5 || centerLayout == CenterControlsLayout.WIDE_SPACED) {
                                    GlassIconButton(
                                        onClick = {
                                            showControls = true
                                            isAutoHideTransition = false
                                            controlsTimeoutKey++
                                            onPreviousEpisode()
                                        },
                                        enabled = hasPreviousEpisode,
                                        size = if (glassCompact) 32.dp else 38.dp,
                                        modifier = Modifier.offset { IntOffset((playerCustomization.hudLayout.prevEpDeltaX * 800f).roundToInt(), (playerCustomization.hudLayout.prevEpDeltaY * 400f).roundToInt()) }.scale(playerCustomization.hudLayout.prevEpScale)
                                    ) {
                                        Icon(
                                            imageVector = playerCustomization.themePack.prevIcon,
                                            contentDescription = "Previous Episode",
                                            tint = Color(0xE6FFFFFF),
                                            modifier = Modifier.size(if (glassCompact) 15.dp else 18.dp)
                                        )
                                    }
                                }

                                if (centerLayout != CenterControlsLayout.PLAY_ONLY) {
                                    GlassIconButton(
                                        onClick = {
                                            showControls = true
                                            isAutoHideTransition = false
                                            controlsTimeoutKey++
                                            PlayerPlaybackPlugin.seekBy(exoPlayer, -10000L)
                                            if (doubleTapSide == "LEFT") {
                                                accumulatedSeekSeconds += 10
                                            } else {
                                                accumulatedSeekSeconds = 10
                                            }
                                            doubleTapSide = "LEFT"
                                            doubleTapTriggerKey++
                                            val curKey = doubleTapTriggerKey
                                            scope.launch {
                                                delay(1200)
                                                if (doubleTapTriggerKey == curKey) {
                                                    doubleTapSide = null
                                                    accumulatedSeekSeconds = 0
                                                    controlsTimeoutKey++
                                                }
                                            }
                                        },
                                        size = if (glassCompact) 38.dp else 46.dp,
                                        modifier = Modifier.offset { IntOffset((playerCustomization.hudLayout.rewindDeltaX * 800f).roundToInt(), (playerCustomization.hudLayout.rewindDeltaY * 400f).roundToInt()) }.scale(playerCustomization.hudLayout.rewindScale)
                                    ) {
                                        if (playerCustomization.icons.seekStyle == SeekIconStyle.DEFAULT) {
                                            IosSeek10Icon(
                                                isForward = false,
                                                modifier = Modifier.size(if (glassCompact) 22.dp else 28.dp),
                                                tint = Color(0xE6FFFFFF),
                                                fontSize = if (glassCompact) 8.sp else 9.5.sp
                                            )
                                        } else {
                                            val seekIcon = when (playerCustomization.icons.seekStyle) {
                                                SeekIconStyle.CHEVRONS -> Icons.Default.FastRewind
                                                SeekIconStyle.ROCKETS -> Icons.Default.FastRewind
                                                SeekIconStyle.MINIMAL_ARROWS -> Icons.Default.ChevronLeft
                                                else -> Icons.Default.Replay10
                                            }
                                            Icon(
                                                imageVector = seekIcon,
                                                contentDescription = "Rewind 10s",
                                                tint = Color(0xE6FFFFFF),
                                                modifier = Modifier.size(if (glassCompact) 18.dp else 22.dp)
                                            )
                                        }
                                    }
                                }

                                GlassIconButton(
                                    onClick = {
                                        showControls = true
                                        isAutoHideTransition = false
                                        controlsTimeoutKey++
                                        PlayerPlaybackPlugin.togglePlayPause(exoPlayer, context)
                                    },
                                    isHero = true,
                                    size = if (glassCompact) 44.dp else 56.dp,
                                    modifier = Modifier.offset { IntOffset((playerCustomization.hudLayout.playPauseDeltaX * 800f).roundToInt(), (playerCustomization.hudLayout.playPauseDeltaY * 400f).roundToInt()) }.scale(playerCustomization.hudLayout.playPauseScale) 
                                ) {
                                    AnimatedContent(
                                        targetState = isPlaying,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(140)) + scaleIn(initialScale = 0.8f, animationSpec = tween(140)))
                                                .togetherWith(fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.8f, animationSpec = tween(100)))
                                        },
                                        label = "heroPlayPauseMorph"
                                    ) { playing ->
                                        GlassPlayPauseGlyph(
                                            isPlaying = playing,
                                            modifier = Modifier.size(if (glassCompact) 18.dp else 24.dp),
                                            tint = Color.White
                                        )
                                    }
                                }

                                if (centerLayout != CenterControlsLayout.PLAY_ONLY) {
                                    GlassIconButton(
                                        onClick = {
                                            showControls = true
                                            isAutoHideTransition = false
                                            controlsTimeoutKey++
                                            PlayerPlaybackPlugin.seekBy(exoPlayer, 10000L)
                                            if (doubleTapSide == "RIGHT") {
                                                accumulatedSeekSeconds += 10
                                            } else {
                                                accumulatedSeekSeconds = 10
                                            }
                                            doubleTapSide = "RIGHT"
                                            doubleTapTriggerKey++
                                            val curKey = doubleTapTriggerKey
                                            scope.launch {
                                                delay(1200)
                                                if (doubleTapTriggerKey == curKey) {
                                                    doubleTapSide = null
                                                    accumulatedSeekSeconds = 0
                                                    controlsTimeoutKey++
                                                }
                                            }
                                        },
                                        size = if (glassCompact) 38.dp else 46.dp,
                                        modifier = Modifier.offset { IntOffset((playerCustomization.hudLayout.forwardDeltaX * 800f).roundToInt(), (playerCustomization.hudLayout.forwardDeltaY * 400f).roundToInt()) }.scale(playerCustomization.hudLayout.forwardScale)
                                    ) {
                                        if (playerCustomization.icons.seekStyle == SeekIconStyle.DEFAULT) {
                                            IosSeek10Icon(
                                                isForward = true,
                                                modifier = Modifier.size(if (glassCompact) 22.dp else 28.dp),
                                                tint = Color(0xE6FFFFFF),
                                                fontSize = if (glassCompact) 8.sp else 9.5.sp
                                            )
                                        } else {
                                            val seekIcon = when (playerCustomization.icons.seekStyle) {
                                                SeekIconStyle.CHEVRONS -> Icons.Default.FastForward
                                                SeekIconStyle.ROCKETS -> Icons.Default.FastForward
                                                SeekIconStyle.MINIMAL_ARROWS -> Icons.Default.ChevronRight
                                                else -> Icons.Default.Forward10
                                            }
                                            Icon(
                                                imageVector = seekIcon,
                                                contentDescription = "Forward 10s",
                                                tint = Color(0xE6FFFFFF),
                                                modifier = Modifier.size(if (glassCompact) 18.dp else 22.dp)
                                            )
                                        }
                                    }
                                }

                                if (centerLayout == CenterControlsLayout.STANDARD_5 || centerLayout == CenterControlsLayout.WIDE_SPACED) {
                                    GlassIconButton(
                                        onClick = {
                                            showControls = true
                                            isAutoHideTransition = false
                                            controlsTimeoutKey++
                                            onNextEpisode()
                                        },
                                        enabled = hasNextEpisode,
                                        size = if (glassCompact) 32.dp else 38.dp,
                                        modifier = Modifier.offset { IntOffset((playerCustomization.hudLayout.nextEpDeltaX * 800f).roundToInt(), (playerCustomization.hudLayout.nextEpDeltaY * 400f).roundToInt()) }.scale(playerCustomization.hudLayout.nextEpScale)
                                    ) {
                                        Icon(
                                            imageVector = playerCustomization.themePack.nextIcon,
                                            contentDescription = "Next Episode",
                                            tint = Color(0xE6FFFFFF),
                                            modifier = Modifier.size(if (glassCompact) 15.dp else 18.dp)
                                        )
                                    }
                                }
                        }
                    }

                    // 3b. Right-Side Unified Vertical Control Group
                    AnimatedVisibility(
                        visible = showControls && !isScreenLocked && !isInPip,
                        enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.92f, animationSpec = tween(140)),
                        exit = if (isAutoHideTransition) {
                            fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180))
                        } else {
                            fadeOut(tween(110))
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = if (glassCompact) 12.dp else 24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(if (glassCompact) 8.dp else 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val rightBtnSize = if (glassCompact) 34.dp else 40.dp
                            val rightIconSize = if (glassCompact) 16.dp else 19.dp

                            // Fullscreen
                            GlassIconButton(
                                size = rightBtnSize,
                                onClick = {
                                    showControls = true
                                    controlsTimeoutKey++
                                    onToggleFullscreen()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) playerCustomization.themePack.exitFullscreenIcon else playerCustomization.themePack.fullscreenIcon,
                                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(rightIconSize)
                                )
                            }

                            // Audio Track Button
                            val isAudioSelected = audioTracks.any { it.isSelected } || isDub
                            GlassIconButton(
                                size = rightBtnSize,
                                onClick = {
                                    showControls = true
                                    controlsTimeoutKey++
                                    activeSettingsTab = PlayerSettingsTab.AUDIO
                                    showPlayerSettingsModal = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GraphicEq,
                                    contentDescription = "Audio Tracks",
                                    tint = if (isAudioSelected) playerCustomization.accentColor.color else Color.White,
                                    modifier = Modifier.size(rightIconSize)
                                )
                            }

                            // Subtitles Button
                            val isCaptionActive = isCaptionsEnabled && (activeCuesText != null || activeLoadedSubtitleUrl != null || subtitleTracks.any { it.isSelected })
                            GlassIconButton(
                                size = rightBtnSize,
                                onClick = {
                                    showControls = true
                                    controlsTimeoutKey++
                                    showCaptionModal = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Subtitles,
                                    contentDescription = "Subtitles",
                                    tint = if (isCaptionActive) playerCustomization.accentColor.color else Color.White,
                                    modifier = Modifier.size(rightIconSize)
                                )
                            }

                            // Speed Button
                            GlassIconButton(
                                size = rightBtnSize,
                                onClick = {
                                    showControls = true
                                    controlsTimeoutKey++
                                    activeSettingsTab = PlayerSettingsTab.SPEED
                                    showPlayerSettingsModal = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Speed,
                                    contentDescription = "Playback Speed",
                                    tint = if (playbackSpeed != 1.0f) playerCustomization.accentColor.color else Color.White,
                                    modifier = Modifier.size(rightIconSize)
                                )
                            }
                        }
                    }

                    // 4. Bottom Bar: Scrubber & Time (YouTube fade & morphing timeline)
                    AnimatedVisibility(
                        visible = showControls && !isScreenLocked && !isInPip,
                        enter = slideInVertically(
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(tween(140, easing = LinearEasing)),
                        exit = if (isAutoHideTransition) {
                            slideOutVertically(
                                animationSpec = tween(220, easing = FastOutLinearInEasing),
                                targetOffsetY = { it / 3 }
                            ) + fadeOut(tween(240, easing = LinearEasing))
                        } else {
                            fadeOut(tween(130, easing = LinearEasing))
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .offset { IntOffset(0, (playerCustomization.hudLayout.seekbarDeltaY * 400f).roundToInt()) }
                            .scale(playerCustomization.hudLayout.seekbarScale)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = if (glassCompact) 12.dp else 20.dp, end = if (glassCompact) 12.dp else 20.dp, top = 4.dp, bottom = if (glassCompact) 4.dp else 8.dp)
                        ) {
                            val timeTextAlpha by animateFloatAsState(
                                targetValue = if (showControls && !isAutoHideTransition) 1f else if (showControls) 0.5f else 0f,
                                animationSpec = tween(if (isAutoHideTransition) 180 else 100),
                                label = "timeTextAlpha"
                            )
                            val animatedSeekbarHeight by animateFloatAsState(
                                targetValue = if (showControls && !isAutoHideTransition) playerCustomization.placement.seekbarHeightDp else 2.5f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                label = "animatedSeekbarHeight"
                            )
                            // Episode info hierarchy (full width with generous breathing room)
                            val hasCustomEpName = !episodeTitle.isNullOrBlank() && !episodeTitle.equals("Episode $episode", ignoreCase = true)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = if (hasCustomEpName) "Episode $episode: $episodeTitle" else "Episode $episode",
                                    fontSize = if (glassCompact) 11.sp else 12.5.sp,
                                    color = Color(0xB3FFFFFF),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = title,
                                    fontSize = if (glassCompact) 15.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Time bar: elapsed | seekbar | remaining
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = formatTime(if (isScrubbing) scrubPositionMs else currentPositionMs),
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.graphicsLayer { alpha = timeTextAlpha }
                                )

                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                val totalWidth = maxWidth
                                val activePosMs = if (isScrubbing) scrubPositionMs else currentPositionMs
                                val playedFrac = if (durationMs > 0L) (activePosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                                val bufferedFrac = if (durationMs > 0L) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

                                // Custom Seekbar Track & Thumb Renderer
                                CineSeekbarRenderer(
                                    style = playerCustomization.seekbarStyle,
                                    palette = themePalette,
                                    accentColor = playerCustomization.accentColor.color,
                                    playedFrac = playedFrac,
                                    bufferedFrac = bufferedFrac,
                                    isScrubbing = isScrubbing,
                                    heightDp = animatedSeekbarHeight,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // AniSkip Segment Highlights Overlay (Clean translucent white)
                                if (durationMs > 0L && skipIntervals.isNotEmpty()) {
                                    skipIntervals.forEach { interval ->
                                        val startFrac = (interval.startTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                        val endFrac = (interval.endTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                        val widthFrac = (endFrac - startFrac).coerceAtLeast(0f)
                                        if (widthFrac > 0.005f) {
                                            val isIntro = interval.skipType.equals("op", ignoreCase = true) || interval.skipType.equals("recap", ignoreCase = true)
                                            val highlightColor = if (isIntro) Color(0x60FFFFFF) else Color(0x35FFFFFF)
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = totalWidth * startFrac)
                                                    .width(totalWidth * widthFrac)
                                                    .height(playerCustomization.placement.seekbarHeightDp.dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(highlightColor)
                                            )
                                        }
                                    }
                                }

                                // Live Seek Tooltip Bubble while scrubbing
                                if (isScrubbing && durationMs > 0L) {
                                    val bubbleOffset = (totalWidth * playedFrac) - 24.dp
                                    Box(
                                        modifier = Modifier
                                            .padding(start = bubbleOffset.coerceAtLeast(0.dp))
                                            .align(Alignment.TopStart)
                                            .offset(y = (-12).dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xEE0A0E1A))
                                            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = formatTime(scrubPositionMs),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.90f)
                                            )
                                        }
                                    }

                                // Touch Slider (Transparent thumb so our custom circular pointer displays)
                                Slider(
                                    value = playedFrac,
                                    onValueChange = { frac ->
                                        controlsTimeoutKey++
                                        isScrubbing = true
                                        scrubPositionMs = (frac * durationMs).toLong().coerceIn(0L, durationMs)
                                    },
                                    onValueChangeFinished = {
                                        isScrubbing = false
                                        currentPositionMs = scrubPositionMs
                                        PlayerPlaybackPlugin.seekTo(exoPlayer, scrubPositionMs)
                                        controlsTimeoutKey++
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Transparent,
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                                }

                                Text(
                                    text = if (showTimeRemaining && durationMs > 0L) {
                                        val rem = (durationMs - (if (isScrubbing) scrubPositionMs else currentPositionMs)).coerceAtLeast(0L)
                                        "-${formatTime(rem)}"
                                    } else {
                                        formatTime(durationMs)
                                    },
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .graphicsLayer { alpha = timeTextAlpha }
                                        .clickable { showTimeRemaining = !showTimeRemaining }
                                )
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
            resizeMode = resizeMode,
            onSelectResizeMode = { mode: Int ->
                notifyFitChange(mode)
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
            availableServers = availableServers,
            currentStreamUrl = streamUrl,
            onSelectServer = { srv ->
                onSelectServer?.invoke(srv)
            },
            serverName = serverName,
            isDub = isDub,
            hasDubAvailable = hasDubAvailable,
            onSwitchAudioStream = onSwitchAudioStream,
            videoFilters = videoFilters,
            onVideoFiltersChange = { newFilters ->
                videoFilters = newFilters
                storageManager.saveVideoFilters(newFilters)
            },
            upscalerSettings = upscalerSettings,
            onUpscalerChange = { newSettings ->
                upscalerSettings = newSettings
                storageManager.saveUpscalerSettings(newSettings)
            }
        )
    }

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
                        audio = if (isDub) "dub" else "sub",
                        activeServerName = serverName
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

    // Modern Subtitles Selector Modal (Image 1)
    if (showCaptionModal) {
        SubtitlesModal(
            availableSubtitles = availableExternal,
            isCaptionsEnabled = isCaptionsEnabled,
            activeCaptionTitle = activeCaptionTitle,
            activeSubtitleUrl = activeLoadedSubtitleUrl,
            downloadingSubtitleId = downloadingSubtitleId,
            onSelectSubtitle = { sub ->
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
            },
            onTurnOffCaptions = {
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
            },
            onOpenSettings = {
                showCaptionModal = false
                showCaptionSettingsModal = true
            },
            onOpenDialoguePanel = {
                showCaptionModal = false
                showDialoguePanel = true
            },
            onUploadClick = { uploadSubLauncher.launch("*/*") },
            onDismiss = { showCaptionModal = false },
            anchor = morphAnchor
        )
    }

    // Full Subtitle / Caption Settings Modal (Images 2, 3, 4)
    if (showCaptionSettingsModal) {
        CaptionSettingsModal(
            captionConfig = captionConfig,
            onConfigChange = { newCfg -> captionConfig = newCfg },
            isCaptionsEnabled = isCaptionsEnabled,
            onToggleCaptions = { isCaptionsEnabled = it },
            subSyncMs = subSyncMs,
            onAdjustSync = { delta -> subSyncMs += delta },
            onResetSync = { subSyncMs = 0L },
            onDismiss = { showCaptionSettingsModal = false }
        )
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
    resizeMode: Int,
    onSelectResizeMode: (Int) -> Unit,
    playbackSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    audioTracks: List<AudioTrackInfo>,
    onSelectAudio: (AudioTrackInfo) -> Unit,
    autoPlayNextEnabled: Boolean,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    autoSkipIntroOutroEnabled: Boolean,
    onToggleAutoSkipIntroOutro: (Boolean) -> Unit,
    availableServers: List<StreamServerOption> = emptyList(),
    currentStreamUrl: String = "",
    onSelectServer: (StreamServerOption) -> Unit = {},
    serverName: String = "",
    isDub: Boolean = false,
    hasDubAvailable: Boolean = true,
    onSwitchAudioStream: ((Boolean) -> Unit)? = null,
    videoFilters: VideoFiltersState = VideoFiltersState(),
    onVideoFiltersChange: (VideoFiltersState) -> Unit = {},
    upscalerSettings: UpscalerState = UpscalerState(),
    onUpscalerChange: (UpscalerState) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAutoServer by remember { mutableStateOf(true) }

    GlassDialog(
        anchor = anchor,
        onDismiss = onDismiss
    ) {
        Box(
            modifier = modifier
                .widthIn(max = 340.dp)
                .wrapContentHeight()
                .glassPanel()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (activeTab) {
                PlayerSettingsTab.MAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Settings",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 6.dp)
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

                        // 1. Server & Audio Track (Always visible in CinePlayer)
                        val serverSubtitle = if (isAutoServer) {
                            if (serverName.isNotBlank()) "Auto ($serverName)" else "Auto (Best)"
                        } else {
                            serverName.ifBlank { "Manual" }
                        }
                        SettingsItemRow(
                            icon = Icons.Default.Dns,
                            title = "Server & Audio",
                            subtitle = serverSubtitle,
                            onClick = { onTabChange(PlayerSettingsTab.SERVER) }
                        )

                        // 2. Audio Track details — Always visible across all servers
                        val activeAudioSubtitle = when {
                            audioTracks.size > 1 -> audioTracks.find { it.isSelected }?.label ?: (if (isDub) "English (Dub)" else "Japanese (Original)")
                            serverName.contains("megaplay", ignoreCase = true) || serverName.contains("vidstream", ignoreCase = true) ->
                                if (isDub) "English (Dub)" else "Japanese (Sub)"
                            else -> if (isDub) "English (Dub)" else "Japanese (Native)"
                        }
                        SettingsItemRow(
                            icon = Icons.Outlined.GraphicEq,
                            title = "Audio Track",
                            subtitle = activeAudioSubtitle,
                            onClick = { onTabChange(PlayerSettingsTab.AUDIO) }
                        )

                        // 3. Captions
                        SettingsItemRow(
                            icon = Icons.Outlined.Subtitles,
                            title = "Captions",
                            onClick = onOpenCaptions
                        )

                        // 4. Playback Speed
                        SettingsItemRow(
                            icon = Icons.Outlined.Speed,
                            title = "Playback Speed",
                            subtitle = "${playbackSpeed}x",
                            onClick = { onTabChange(PlayerSettingsTab.SPEED) }
                        )

                        // 5. Aspect Ratio / Video Fit
                        val fitSubtitle = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop to Fill / Zoom"
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                            else -> "Fit to Screen"
                        }
                        SettingsItemRow(
                            icon = Icons.Default.AspectRatio,
                            title = "Aspect Ratio",
                            subtitle = fitSubtitle,
                            onClick = { onTabChange(PlayerSettingsTab.ORIENTATION) }
                        )

                        // 6. AI Upscaler (Clean standard row, subtitle shows mode)
                        val upscalerSubtitle = if (upscalerSettings.enabled) {
                            val m = ANIME4K_MODES.find { it.id == upscalerSettings.mode }?.label ?: "Mode A"
                            "4K · $m"
                        } else {
                            "Off"
                        }
                        SettingsItemRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "AI Upscaler",
                            subtitle = upscalerSubtitle,
                            onClick = { onTabChange(PlayerSettingsTab.UPSCALER) }
                        )

                        // 7. Visual Enhancements & Shaders
                        val vfxSubtitle = if (!videoFilters.isDefault) {
                            val pName = VIDEO_FILTER_PRESETS.find { it.id == videoFilters.preset }?.label ?: "Custom"
                            pName
                        } else {
                            "Default"
                        }
                        SettingsItemRow(
                            icon = Icons.Default.Tune,
                            title = "Visual Enhancements",
                            subtitle = vfxSubtitle,
                            onClick = { onTabChange(PlayerSettingsTab.PICTURE) }
                        )

                        // 8. Player Gestures
                        val g = currentCustomization.gestures
                        val activeGesturesCount = listOf(
                            g.volumeEnabled,
                            g.brightnessEnabled,
                            g.swipeSeekEnabled,
                            g.holdTo2xEnabled,
                            g.doubleTapSeekEnabled,
                            g.pinchToZoomEnabled
                        ).count { it }
                        val gestureSubtitle = when {
                            !g.masterEnabled || activeGesturesCount == 0 -> "Gestures disabled"
                            activeGesturesCount == 6 -> "All gestures enabled"
                            else -> "$activeGesturesCount of 6 enabled"
                        }
                        SettingsItemRow(
                            icon = Icons.Default.TouchApp,
                            title = "Player Gestures",
                            subtitle = gestureSubtitle,
                            onClick = { onTabChange(PlayerSettingsTab.GESTURES) }
                        )

                        // 9. Player Themes & Colors
                        SettingsItemRow(
                            icon = Icons.Default.DashboardCustomize,
                            title = "Player Themes & Colors",
                            subtitle = currentCustomization.themePack.displayName,
                            onClick = { onTabChange(PlayerSettingsTab.THEME_PACKS) }
                        )

                        // 10. Auto-Play Next
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleAutoPlayNext(!autoPlayNextEnabled) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Auto-Play Next",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (autoPlayNextEnabled) "Plays next episode" else "Pause at end",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            com.nyyrox.aniverse.ui.components.LiquidGlassSwitch(
                                checked = autoPlayNextEnabled,
                                onCheckedChange = { onToggleAutoPlayNext(it) }
                            )
                        }

                        // 11. Auto-Skip Intro/Outro
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleAutoSkipIntroOutro(!autoSkipIntroOutroEnabled) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Auto-Skip Intro & Outro",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (autoSkipIntroOutroEnabled) "Auto skip theme songs" else "Manual skip button",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            com.nyyrox.aniverse.ui.components.LiquidGlassSwitch(
                                checked = autoSkipIntroOutroEnabled,
                                onCheckedChange = { onToggleAutoSkipIntroOutro(it) }
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.QUALITY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubHeader(title = "Quality", onBack = { onTabChange(PlayerSettingsTab.MAIN) }, onClose = onDismiss)
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(Color(0x14FFFFFF)))

                        if (qualityTracks.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Auto (Native Quality)", color = OttPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            qualityTracks.forEach { track ->
                                val isSelected = track.isSelected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                        .clickable {
                                            onSelectQuality(track)
                                            onDismiss()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = track.label,
                                        color = if (isSelected) OttPrimary else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.SERVER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubHeader(title = "Server & Audio", onBack = { onTabChange(PlayerSettingsTab.MAIN) }, onClose = onDismiss)
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(Color(0x14FFFFFF)))

                        // 1. Auto-Select Master Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isAutoServer = !isAutoServer }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Auto-Select Best Server",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isAutoServer) "Active · Auto-routes to lowest latency" else "Manual server selection enabled",
                                        color = if (isAutoServer) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            com.nyyrox.aniverse.ui.components.LiquidGlassSwitch(
                                checked = isAutoServer,
                                onCheckedChange = { isAutoServer = it }
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(Color(0x14FFFFFF)))

                        // 2. Audio Version (Sub / Dub)
                        Text(
                            text = "AUDIO VERSION",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sub Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isDub) Color(0x28FFFFFF) else Color(0x0EFFFFFF))
                                    .clickable {
                                        if (isDub) {
                                            onSwitchAudioStream?.invoke(false)
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!isDub) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "Sub (Japanese)",
                                        color = if (!isDub) OttPrimary else Color(0xCCFFFFFF),
                                        fontSize = 13.sp,
                                        fontWeight = if (!isDub) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            // Dub Button
                            val dubAvailable = hasDubAvailable
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDub) Color(0x28FFFFFF) else Color(0x0EFFFFFF))
                                    .clickable(enabled = dubAvailable) {
                                        if (!isDub && dubAvailable) {
                                            onSwitchAudioStream?.invoke(true)
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isDub) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = if (dubAvailable) "Dub (English)" else "Dub (N/A)",
                                        color = when {
                                            !dubAvailable -> Color(0x40FFFFFF)
                                            isDub -> OttPrimary
                                            else -> Color(0xCCFFFFFF)
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isDub) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(Color(0x14FFFFFF)))

                        // 3. Available Servers List
                        Text(
                            text = "AVAILABLE SERVERS",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        if (isAutoServer) {
                            Text(
                                text = "Auto mode is active. Tap any server to lock manual playback.",
                                color = Color(0xFF64748B),
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        if (availableServers.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Default Direct Stream", color = OttPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            availableServers.forEach { srv ->
                                val isSelected = (!isAutoServer && (srv.streamUrl == currentStreamUrl || srv.serverName.equals(serverName, ignoreCase = true))) ||
                                        (isAutoServer && srv.serverName.equals(serverName, ignoreCase = true))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                        .clickable {
                                            isAutoServer = false
                                            onSelectServer(srv)
                                            onDismiss()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = srv.serverName,
                                            color = if (isSelected) OttPrimary else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = if (srv.isHls) "Direct HLS Stream · Low Latency" else "Direct Video Stream",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.ORIENTATION -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubHeader(title = "Orientation / Video Fit", onBack = { onTabChange(PlayerSettingsTab.MAIN) }, onClose = onDismiss)
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(Color(0x14FFFFFF)))

                        val options = listOf(
                            Triple(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit to Screen", "Preserve original aspect ratio"),
                            Triple(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Crop to Fill / Zoom", "Fill display by zooming"),
                            Triple(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Stretch", "Stretch video to edges")
                        )

                        options.forEach { (mode, title, sub) ->
                            val isSelected = resizeMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                    .clickable {
                                        onSelectResizeMode(mode)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) OttPrimary else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(text = sub, color = OttSecondary, fontSize = 11.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.SPEED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubHeader(title = "Playback Speed", onBack = { onTabChange(PlayerSettingsTab.MAIN) }, onClose = onDismiss)
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(Color(0x14FFFFFF)))

                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            val isSelected = playbackSpeed == spd
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                    .clickable {
                                        onSelectSpeed(spd)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (spd == 1.0f) "1.0x (Normal)" else "${spd}x",
                                    color = if (isSelected) OttPrimary else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.AUDIO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubHeader(title = "Audio Track", onBack = { onTabChange(PlayerSettingsTab.MAIN) }, onClose = onDismiss)
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(Color(0x14FFFFFF)))

                        val isSingleAudioServer = serverName.contains("megaplay", ignoreCase = true) ||
                                serverName.contains("vidstream", ignoreCase = true)

                        if (isSingleAudioServer) {
                            // MegaPlay & Vidstream: single audio stream output (separate Sub and Dub streams)
                            val options = listOf(
                                Triple(false, "Japanese (Sub)", true),
                                Triple(true, "English (Dub)", hasDubAvailable)
                            )
                            options.forEach { (targetDub, label, isAvailable) ->
                                val isSelected = (isDub == targetDub)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                        .clickable {
                                            if (!isAvailable) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Dub is not available for this episode",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                if (isDub != targetDub) {
                                                    onSwitchAudioStream?.invoke(targetDub)
                                                }
                                                onDismiss()
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            color = when {
                                                !isAvailable -> Color.White.copy(alpha = 0.38f)
                                                isSelected -> OttPrimary
                                                else -> Color.White
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (!isAvailable) {
                                            Text(
                                                text = "Unavailable on this server for this episode",
                                                color = Color.White.copy(alpha = 0.30f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        } else if (audioTracks.size > 1) {
                            // ACDN & FlixCloud: multi-audio HLS streams, list all available audio tracks
                            audioTracks.forEach { track ->
                                val isSelected = track.isSelected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                        .clickable {
                                            onSelectAudio(track)
                                            onDismiss()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = track.label,
                                        color = if (isSelected) OttPrimary else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        } else {
                            // Single track / fallback for ACDN/FlixCloud
                            val options = listOf(
                                Triple(false, "Japanese (Native)", true),
                                Triple(true, "English (Dub)", hasDubAvailable)
                            )
                            options.forEach { (targetDub, label, isAvailable) ->
                                val isSelected = (isDub == targetDub)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                        .clickable {
                                            if (!isAvailable) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Dub is not available for this episode",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                if (isDub != targetDub) {
                                                    onSwitchAudioStream?.invoke(targetDub)
                                                }
                                                onDismiss()
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            color = when {
                                                !isAvailable -> Color.White.copy(alpha = 0.38f)
                                                isSelected -> OttPrimary
                                                else -> Color.White
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (!isAvailable) {
                                            Text(
                                                text = "Unavailable on this server for this episode",
                                                color = Color.White.copy(alpha = 0.30f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.THEME_PACKS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubHeader(
                            title = "Player Theme Packs",
                            onBack = { onTabChange(PlayerSettingsTab.MAIN) },
                            onClose = onDismiss
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(1.dp)
                                .background(Color(0x14FFFFFF))
                        )

                        PlayerThemePack.values().forEach { pack ->
                            val isSelected = currentCustomization.themePack == pack
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) currentCustomization.accentColor.color.copy(alpha = 0.20f) else Color.Transparent)
                                    .clickable {
                                        val newSettings = pack.applyTo(currentCustomization)
                                        onApplyCustomization(newSettings)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x15FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (pack) {
                                                PlayerThemePack.ZENKAI_CLASSIC -> Icons.Default.PlayCircle
                                                PlayerThemePack.CYBER_TECH -> Icons.Default.FastForward
                                                PlayerThemePack.SAMURAI_BLADE -> Icons.Default.PlayArrow
                                                PlayerThemePack.MINIMAL_NEO -> Icons.Default.PlayArrow
                                                PlayerThemePack.RETRO_ARCADE -> Icons.Default.PlayCircle
                                            },
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.90f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = pack.displayName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = pack.description,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(1.dp)
                                .background(Color(0x14FFFFFF))
                        )

                        Text(
                            text = "PLAYER ACCENT COLOR",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerAccentColor.entries.forEach { accent ->
                                val isColorSelected = currentCustomization.accentColor == accent
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accent.color)
                                        .border(
                                            width = if (isColorSelected) 2.5.dp else 1.dp,
                                            color = if (isColorSelected) Color.White else Color(0x35FFFFFF),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            val newSettings = currentCustomization.copy(accentColor = accent)
                                            onApplyCustomization(newSettings)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isColorSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = accent.displayName,
                                            tint = if (accent == PlayerAccentColor.PURE_WHITE) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.GESTURES -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubHeader(
                            title = "Player Gestures",
                            onBack = { onTabChange(PlayerSettingsTab.MAIN) },
                            onClose = onDismiss
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(1.dp)
                                .background(Color(0x14FFFFFF))
                        )

                        val g = currentCustomization.gestures

                        // Master Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val targetMaster = !g.masterEnabled
                                    val newGestures = if (targetMaster) {
                                        g.copy(
                                            masterEnabled = true,
                                            volumeEnabled = true,
                                            brightnessEnabled = true,
                                            swipeSeekEnabled = true,
                                            holdTo2xEnabled = true,
                                            doubleTapSeekEnabled = true,
                                            pinchToZoomEnabled = true
                                        )
                                    } else {
                                        g.copy(masterEnabled = false)
                                    }
                                    onApplyCustomization(currentCustomization.copy(gestures = newGestures))
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Enable All Gestures",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (g.masterEnabled) "All gestures enabled" else "Master switch: gestures off",
                                    color = if (g.masterEnabled) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
                                )
                            }
                            com.nyyrox.aniverse.ui.components.LiquidGlassSwitch(
                                checked = g.masterEnabled,
                                onCheckedChange = { isChecked ->
                                    val newGestures = if (isChecked) {
                                        g.copy(
                                            masterEnabled = true,
                                            volumeEnabled = true,
                                            brightnessEnabled = true,
                                            swipeSeekEnabled = true,
                                            holdTo2xEnabled = true,
                                            doubleTapSeekEnabled = true,
                                            pinchToZoomEnabled = true
                                        )
                                    } else {
                                        g.copy(masterEnabled = false)
                                    }
                                    onApplyCustomization(currentCustomization.copy(gestures = newGestures))
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(1.dp)
                                .background(Color(0x14FFFFFF))
                        )

                        Text(
                            text = "INDIVIDUAL GESTURES",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        GestureToggleRow(
                            icon = Icons.Default.VolumeUp,
                            title = "Volume Gesture",
                            subtitle = "Swipe right side vertically to adjust volume",
                            checked = g.masterEnabled && g.volumeEnabled,
                            masterEnabled = g.masterEnabled,
                            onCheckedChange = { isChecked ->
                                val newGestures = g.copy(
                                    masterEnabled = if (isChecked) true else g.masterEnabled,
                                    volumeEnabled = isChecked
                                )
                                onApplyCustomization(currentCustomization.copy(gestures = newGestures))
                            }
                        )

                        GestureToggleRow(
                            icon = Icons.Default.BrightnessMedium,
                            title = "Brightness Gesture",
                            subtitle = "Swipe left side vertically to adjust brightness",
                            checked = g.masterEnabled && g.brightnessEnabled,
                            masterEnabled = g.masterEnabled,
                            onCheckedChange = { isChecked ->
                                val newGestures = g.copy(
                                    masterEnabled = if (isChecked) true else g.masterEnabled,
                                    brightnessEnabled = isChecked
                                )
                                onApplyCustomization(currentCustomization.copy(gestures = newGestures))
                            }
                        )

                        GestureToggleRow(
                            icon = Icons.Default.FastForward,
                            title = "Swipe to Seek",
                            subtitle = "Swipe horizontally to scrub forward or backward",
                            checked = g.masterEnabled && g.swipeSeekEnabled,
                            masterEnabled = g.masterEnabled,
                            onCheckedChange = { isChecked ->
                                val newGestures = g.copy(
                                    masterEnabled = if (isChecked) true else g.masterEnabled,
                                    swipeSeekEnabled = isChecked
                                )
                                onApplyCustomization(currentCustomization.copy(gestures = newGestures))
                            }
                        )

                        GestureToggleRow(
                            icon = Icons.Default.Speed,
                            title = "Hold to 2x Speed",
                            subtitle = "Long press anywhere during playback for 2x speed",
                            checked = g.masterEnabled && g.holdTo2xEnabled,
                            masterEnabled = g.masterEnabled,
                            onCheckedChange = { isChecked ->
                                val newGestures = g.copy(
                                    masterEnabled = if (isChecked) true else g.masterEnabled,
                                    holdTo2xEnabled = isChecked
                                )
                                onApplyCustomization(currentCustomization.copy(gestures = newGestures))
                            }
                        )

                        GestureToggleRow(
                            icon = Icons.Default.Forward10,
                            title = "Double Tap Gestures",
                            subtitle = "Double tap sides to seek 10s, double tap center to play/pause",
                            checked = g.masterEnabled && g.doubleTapSeekEnabled,
                            masterEnabled = g.masterEnabled,
                            onCheckedChange = { isChecked ->
                                val newGestures = g.copy(
                                    masterEnabled = if (isChecked) true else g.masterEnabled,
                                    doubleTapSeekEnabled = isChecked
                                )
                                onApplyCustomization(currentCustomization.copy(gestures = newGestures))
                            }
                        )

                        GestureToggleRow(
                            icon = Icons.Default.AspectRatio,
                            title = "Pinch to Change Ratio",
                            subtitle = "Pinch or drag with two fingers to adjust video aspect ratio",
                            checked = g.masterEnabled && g.pinchToZoomEnabled,
                            masterEnabled = g.masterEnabled,
                            onCheckedChange = { isChecked ->
                                val newGestures = g.copy(
                                    masterEnabled = if (isChecked) true else g.masterEnabled,
                                    pinchToZoomEnabled = isChecked
                                )
                                onApplyCustomization(currentCustomization.copy(gestures = newGestures))
                            }
                        )

                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.UPSCALER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubHeader(
                            title = "AI Upscaler",
                            onBack = { onTabChange(PlayerSettingsTab.MAIN) },
                            onClose = onDismiss
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(1.dp)
                                .background(Color(0x14FFFFFF))
                        )

                        // Master Switch with LiquidGlassSwitch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onUpscalerChange(upscalerSettings.copy(enabled = !upscalerSettings.enabled))
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Enable GPU 4K Upscaler",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (upscalerSettings.enabled) "Active · GPU Super-Resolution" else "Real-time edge & line clarity",
                                        color = if (upscalerSettings.enabled) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            com.nyyrox.aniverse.ui.components.LiquidGlassSwitch(
                                checked = upscalerSettings.enabled,
                                onCheckedChange = { isChecked ->
                                    onUpscalerChange(upscalerSettings.copy(enabled = isChecked))
                                }
                            )
                        }

                        if (upscalerSettings.enabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .height(1.dp)
                                    .background(Color(0x14FFFFFF))
                            )

                            Text(
                                text = "QUALITY PRESET",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )

                            ANIME4K_MODES.forEach { modeItem ->
                                val isSelected = upscalerSettings.mode == modeItem.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                        .clickable {
                                            onUpscalerChange(upscalerSettings.copy(mode = modeItem.id))
                                        }
                                        .padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = modeItem.label,
                                            color = if (isSelected) OttPrimary else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = modeItem.desc,
                                            color = OttSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = OttPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .height(1.dp)
                                    .background(Color(0x14FFFFFF))
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Upscale Intensity",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${upscalerSettings.intensity}%",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Slider(
                                value = upscalerSettings.intensity.toFloat(),
                                onValueChange = { newVal ->
                                    onUpscalerChange(upscalerSettings.copy(intensity = newVal.toInt()))
                                },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0x99FFFFFF),
                                    inactiveTrackColor = Color(0x28FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(24.dp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                    }
                }

                PlayerSettingsTab.PICTURE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubHeader(
                            title = "Visual Enhancements",
                            onBack = { onTabChange(PlayerSettingsTab.MAIN) },
                            onClose = onDismiss
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(1.dp)
                                .background(Color(0x14FFFFFF))
                        )

                        Text(
                            text = "COLOR & CONTRAST PRESETS",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        val presetDescriptions = mapOf(
                            "off" to "Standard untouched video colors",
                            "vibrant" to "Richer anime colors and enhanced saturation",
                            "cinema" to "Expanded dynamic range and cinema contrast",
                            "sharp" to "Crisp outlines and edge definition",
                            "warm" to "Reduced blue light with soft amber tones"
                        )

                        VIDEO_FILTER_PRESETS.forEach { p ->
                            val isSelected = videoFilters.preset == p.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                                    .clickable {
                                        onVideoFiltersChange(videoFilters.copy(preset = p.id))
                                    }
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = p.label,
                                        color = if (isSelected) OttPrimary else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = presetDescriptions[p.id] ?: "",
                                        color = OttSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = OttPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(1.dp)
                                .background(Color(0x14FFFFFF))
                        )

                        // Brightness Slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Brightness",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${videoFilters.brightness}%",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Slider(
                            value = videoFilters.brightness.toFloat(),
                            onValueChange = { newVal ->
                                onVideoFiltersChange(videoFilters.copy(brightness = newVal.toInt()))
                            },
                            valueRange = 70f..130f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0x99FFFFFF),
                                inactiveTrackColor = Color(0x28FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(24.dp)
                        )

                        // Contrast Slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contrast",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${videoFilters.contrast}%",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Slider(
                            value = videoFilters.contrast.toFloat(),
                            onValueChange = { newVal ->
                                onVideoFiltersChange(videoFilters.copy(contrast = newVal.toInt()))
                            },
                            valueRange = 70f..140f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0x99FFFFFF),
                                inactiveTrackColor = Color(0x28FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(24.dp)
                        )

                        // Color Saturation Slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Color Saturation",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${videoFilters.saturation}%",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Slider(
                            value = videoFilters.saturation.toFloat(),
                            onValueChange = { newVal ->
                                onVideoFiltersChange(videoFilters.copy(saturation = newVal.toInt()))
                            },
                            valueRange = 70f..160f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0x99FFFFFF),
                                inactiveTrackColor = Color(0x28FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(24.dp)
                        )

                        // Reset visual shaders button
                        if (!videoFilters.isDefault) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x1FFFFFFF))
                                    .clickable {
                                        onVideoFiltersChange(VideoFiltersState())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Reset visual shaders",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) Color.White else Color(0x80FFFFFF),
                modifier = Modifier.size(18.dp)
            )
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
        com.nyyrox.aniverse.ui.components.LiquidGlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
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
            .clip(RoundedCornerShape(10.dp))
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
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
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
