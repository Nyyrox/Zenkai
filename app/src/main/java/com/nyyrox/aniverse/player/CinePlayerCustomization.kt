package com.nyyrox.aniverse.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.specular.PrismalSpecular
import com.styropyr0.prismal.specular.PrismalSpecularStyle
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.LocalPrismalBackdrop
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.view.SurfaceView
import android.view.View
import android.view.TextureView
import android.view.PixelCopy
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import android.os.Build
import android.os.HandlerThread
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ==========================================
// 1. DATA MODELS & ENUMS (CLEAN WHITE MONOCHROME)
// ==========================================

/**
 * Standard Clean Player Palette:
 * Crisp white, subtle slate, transparent glass, zero blue.
 */
data class PlayerThemePalette(
    val primary: Color = Color(0xFFFFFFFF),
    val secondary: Color = Color(0xFFCBD5E1),
    val accentGlow: Color = Color(0xD9FFFFFF),
    val surfaceGlass: Color = Color(0x18FFFFFF),
    val borderGlass: Color = Color(0x28FFFFFF),
    val backgroundOverlay: List<Color> = listOf(Color(0xD9000000), Color(0x20000000), Color(0xD9000000))
) {
    companion object {
        val CLEAN_ZENKAI = PlayerThemePalette()
        fun fromId(ignored: Any? = null): PlayerThemePalette = CLEAN_ZENKAI
    }
}

enum class PlayerThemeId(val displayName: String, val tagline: String) {
    ZENKAI_DEFAULT("Clean Zenkai", "Clean, high-contrast monochrome & glass")
}

enum class SeekTransitionStyle(val displayName: String) {
    CHEVRON_SWEEP("Smooth Chevrons"),
    CYBER_PULSE("Cyber Radar Pulse"),
    BLADE_SLASH("Blade Slash"),
    PIXEL_STEP("Pixel Step"),
    MINIMAL_DOT("Minimalist Dot")
}

enum class LoadingBarStyle(val displayName: String, val description: String) {
    ZENKAI_FLAME("Zenkai Aura Flame", "Fiery pulsating white energy rings"),
    NEON_PULSE("Neon Pulse", "High-glow orbital energy core with particle sweep"),
    CYBER_WAVE("Cyber Matrix Ring", "High-tech rotating segmented radar ring"),
    MINIMAL_ARC("Minimalist Arc", "Ultra sleek smooth single-stroke arc"),
    QUANTUM_DOTS("Quantum Orbit", "Tri-particle spring orbital cluster")
}

enum class SeekbarStyle(val displayName: String, val description: String) {
    GLOW_CAPSULE("Glow Gradient Capsule", "Translucent progress bar with glowing white thumb"),
    MINIMAL_NEON("Minimalist Neon Line", "Sleek razor-thin laser line with subtle pulse glow"),
    CHUNKY_GLASS("Chunky Frosted Glass", "Modern rounded glass rail with high-contrast indicator"),
    CYBER_SEGMENTS("Cyber Segmented Bar", "Tactile futuristic segment markers with chapter ticks"),
    WAVE_ENERGY("Fluid Wave Track", "Smooth flowing dual-tone wave track with liquid shimmer")
}

enum class IconPackPreset(val displayName: String, val description: String) {
    MODERN_MINIMAL("Modern Minimal", "Clean rounded borderless controls"),
    CYBER_NEON("Cyber Neon Glow", "Laser outline controls"),
    FROSTED_GLASS("Frosted Glass Pill", "Subtle glass-backed buttons"),
    ZENKAI_AURA("Zenkai Aura Flare", "Glowing monochrome frame buttons"),
    BLADE_SAMURAI("Blade Sharp", "Angular crisp controls"),
    RETRO_ARCADE("Retro Arcade", "Boxy arcade controls")
}

// Individual Button Icon Style Enums
enum class PlayIconStyle(val displayName: String) {
    DEFAULT("Standard Minimal"),
    CIRCLE_RING("Circle Glass Disc"),
    NEON_BOX("Neon Rounded Box"),
    CYBER_DIAMOND("Cyber Diamond"),
    SHONEN_AURA("Zenkai Aura Ring")
}

enum class SeekIconStyle(val displayName: String) {
    DEFAULT("Curved 10s Counter"),
    CHEVRONS("Double Chevrons"),
    ROCKETS("Speed Rockets"),
    MINIMAL_ARROWS("Minimal Step")
}

enum class LockIconStyle(val displayName: String) {
    SLEEK_PADLOCK("Sleek Padlock"),
    HEX_KEY("Cyber Hex Key"),
    TECH_SHIELD("Aegis Shield"),
    MINIMAL_RING("Minimalist Ring")
}

enum class SubtitleIconStyle(val displayName: String) {
    CLOSED_CAPTION("CC Box"),
    SUBTITLE_LINES("Subtitle Lines"),
    DIALOGUE_BUBBLE("Speech Bubble")
}

enum class AudioIconStyle(val displayName: String) {
    MUSIC_NOTE("Musical Note"),
    HEADPHONES("Studio Headphones"),
    SOUND_WAVES("Graphic Equalizer")
}

enum class SettingsIconStyle(val displayName: String) {
    GEAR("Precision Gear"),
    SLIDERS("Mixing Console"),
    WAVES("Tech Waves")
}

enum class LockScreenAnchor(val displayName: String) {
    TOP_LEFT("Top-Left"),
    TOP_RIGHT("Top-Right"),
    BOTTOM_LEFT("Bottom-Left"),
    BOTTOM_RIGHT("Bottom-Right"),
    TOP_CENTER("Top-Center"),
    CUSTOM_FREEHAND("Custom Position")
}

enum class CenterControlsLayout(val displayName: String) {
    STANDARD_5("Standard 5-Button Deck"),
    COMPACT_3("Compact 3-Button"),
    PLAY_ONLY("Play/Pause Only"),
    WIDE_SPACED("Wide Cinema Spaced")
}

// Player Accent Colors
enum class PlayerAccentColor(
    val id: String,
    val displayName: String,
    val color: Color
) {
    PURE_WHITE("white", "Pure White", Color(0xFFFFFFFF)),
    ZENKAI_CYAN("cyan", "Zenkai Cyan", Color(0xFF38BDF8)),
    CRIMSON_RED("red", "Crimson Red", Color(0xFFEF4444)),
    NEON_PURPLE("purple", "Neon Purple", Color(0xFFA855F7)),
    EMERALD_GREEN("green", "Emerald Green", Color(0xFF10B981)),
    AMBER_GOLD("gold", "Amber Gold", Color(0xFFF59E0B)),
    SAKURA_PINK("pink", "Sakura Pink", Color(0xFFF472B6));

    companion object {
        fun fromId(id: String?): PlayerAccentColor {
            if (id.isNullOrBlank()) return PURE_WHITE
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: PURE_WHITE
        }
    }
}

// Complete Matched Theme Packs
enum class PlayerThemePack(
    val displayName: String,
    val description: String,
    val iconPack: IconPackPreset,
    val playStyle: PlayIconStyle,
    val seekStyle: SeekIconStyle,
    val lockStyle: LockIconStyle,
    val seekTransitionStyle: SeekTransitionStyle,
    val loadingStyle: LoadingBarStyle,
    val seekbarStyle: SeekbarStyle,
    val settingsIcon: ImageVector,
    val subtitleIcon: ImageVector,
    val audioIcon: ImageVector,
    val playlistIcon: ImageVector,
    val fullscreenIcon: ImageVector,
    val exitFullscreenIcon: ImageVector,
    val prevIcon: ImageVector,
    val nextIcon: ImageVector,
    val lockIcon: ImageVector,
    val unlockIcon: ImageVector
) {
    ZENKAI_CLASSIC(
        displayName = "Zenkai Classic",
        description = "Clean rounded icons, smooth chevrons, white aura",
        iconPack = IconPackPreset.MODERN_MINIMAL,
        playStyle = PlayIconStyle.DEFAULT,
        seekStyle = SeekIconStyle.DEFAULT,
        lockStyle = LockIconStyle.SLEEK_PADLOCK,
        seekTransitionStyle = SeekTransitionStyle.CHEVRON_SWEEP,
        loadingStyle = LoadingBarStyle.ZENKAI_FLAME,
        seekbarStyle = SeekbarStyle.GLOW_CAPSULE,
        settingsIcon = Icons.Default.Settings,
        subtitleIcon = Icons.Default.ClosedCaption,
        audioIcon = Icons.Default.Audiotrack,
        playlistIcon = Icons.Default.FormatListBulleted,
        fullscreenIcon = Icons.Default.Fullscreen,
        exitFullscreenIcon = Icons.Default.FullscreenExit,
        prevIcon = Icons.Default.SkipPrevious,
        nextIcon = Icons.Default.SkipNext,
        lockIcon = Icons.Default.Lock,
        unlockIcon = Icons.Default.LockOpen
    ),
    CYBER_TECH(
        displayName = "Cyber Tech",
        description = "Futuristic laser outline icons, radar pulse seek",
        iconPack = IconPackPreset.CYBER_NEON,
        playStyle = PlayIconStyle.CYBER_DIAMOND,
        seekStyle = SeekIconStyle.ROCKETS,
        lockStyle = LockIconStyle.HEX_KEY,
        seekTransitionStyle = SeekTransitionStyle.CYBER_PULSE,
        loadingStyle = LoadingBarStyle.CYBER_WAVE,
        seekbarStyle = SeekbarStyle.CYBER_SEGMENTS,
        settingsIcon = Icons.Default.Tune,
        subtitleIcon = Icons.Default.Subtitles,
        audioIcon = Icons.Default.Headphones,
        playlistIcon = Icons.Default.ViewList,
        fullscreenIcon = Icons.Default.ZoomOutMap,
        exitFullscreenIcon = Icons.Default.FullscreenExit,
        prevIcon = Icons.Default.FirstPage,
        nextIcon = Icons.Default.LastPage,
        lockIcon = Icons.Default.VpnKey,
        unlockIcon = Icons.Default.LockOpen
    ),
    SAMURAI_BLADE(
        displayName = "Samurai Blade",
        description = "Angular crisp controls, swift slash seek transition",
        iconPack = IconPackPreset.BLADE_SAMURAI,
        playStyle = PlayIconStyle.NEON_BOX,
        seekStyle = SeekIconStyle.MINIMAL_ARROWS,
        lockStyle = LockIconStyle.TECH_SHIELD,
        seekTransitionStyle = SeekTransitionStyle.BLADE_SLASH,
        loadingStyle = LoadingBarStyle.QUANTUM_DOTS,
        seekbarStyle = SeekbarStyle.WAVE_ENERGY,
        settingsIcon = Icons.Default.AutoAwesome,
        subtitleIcon = Icons.Default.Translate,
        audioIcon = Icons.Default.VolumeUp,
        playlistIcon = Icons.Default.Layers,
        fullscreenIcon = Icons.Default.OpenWith,
        exitFullscreenIcon = Icons.Default.FullscreenExit,
        prevIcon = Icons.Default.NavigateBefore,
        nextIcon = Icons.Default.NavigateNext,
        lockIcon = Icons.Default.Shield,
        unlockIcon = Icons.Default.Security
    ),
    MINIMAL_NEO(
        displayName = "Minimal Neo",
        description = "Ultra sleek borderless glass, subtle clean chevrons",
        iconPack = IconPackPreset.FROSTED_GLASS,
        playStyle = PlayIconStyle.CIRCLE_RING,
        seekStyle = SeekIconStyle.DEFAULT,
        lockStyle = LockIconStyle.MINIMAL_RING,
        seekTransitionStyle = SeekTransitionStyle.MINIMAL_DOT,
        loadingStyle = LoadingBarStyle.MINIMAL_ARC,
        seekbarStyle = SeekbarStyle.MINIMAL_NEON,
        settingsIcon = Icons.Default.MoreVert,
        subtitleIcon = Icons.Default.ShortText,
        audioIcon = Icons.Default.GraphicEq,
        playlistIcon = Icons.Default.Reorder,
        fullscreenIcon = Icons.Default.Fullscreen,
        exitFullscreenIcon = Icons.Default.FullscreenExit,
        prevIcon = Icons.Default.ArrowBack,
        nextIcon = Icons.Default.ArrowForward,
        lockIcon = Icons.Default.Lock,
        unlockIcon = Icons.Default.LockOpen
    ),
    RETRO_ARCADE(
        displayName = "Retro Arcade",
        description = "Boxy tactile arcade controls, stepped arrow seek",
        iconPack = IconPackPreset.RETRO_ARCADE,
        playStyle = PlayIconStyle.SHONEN_AURA,
        seekStyle = SeekIconStyle.DEFAULT,
        lockStyle = LockIconStyle.SLEEK_PADLOCK,
        seekTransitionStyle = SeekTransitionStyle.PIXEL_STEP,
        loadingStyle = LoadingBarStyle.NEON_PULSE,
        seekbarStyle = SeekbarStyle.CHUNKY_GLASS,
        settingsIcon = Icons.Default.Dashboard,
        subtitleIcon = Icons.Default.Forum,
        audioIcon = Icons.Default.MusicNote,
        playlistIcon = Icons.Default.Apps,
        fullscreenIcon = Icons.Default.Fullscreen,
        exitFullscreenIcon = Icons.Default.FullscreenExit,
        prevIcon = Icons.Default.FastRewind,
        nextIcon = Icons.Default.FastForward,
        lockIcon = Icons.Default.Lock,
        unlockIcon = Icons.Default.LockOpen
    );

    fun applyTo(current: CinePlayerCustomizationSettings): CinePlayerCustomizationSettings {
        return current.copy(
            themePack = this,
            loadingStyle = this.loadingStyle,
            seekbarStyle = this.seekbarStyle,
            icons = current.icons.copy(
                iconPack = this.iconPack,
                playStyle = this.playStyle,
                seekStyle = this.seekStyle,
                lockStyle = this.lockStyle
            )
        )
    }
}

data class HudLayoutConfig(
    val lockDeltaX: Float = 0f,
    val lockDeltaY: Float = 0f,
    val lockScale: Float = 1.0f,

    val playPauseDeltaX: Float = 0f,
    val playPauseDeltaY: Float = 0f,
    val playPauseScale: Float = 1.0f,

    val rewindDeltaX: Float = 0f,
    val rewindDeltaY: Float = 0f,
    val rewindScale: Float = 1.0f,

    val forwardDeltaX: Float = 0f,
    val forwardDeltaY: Float = 0f,
    val forwardScale: Float = 1.0f,

    val prevEpDeltaX: Float = 0f,
    val prevEpDeltaY: Float = 0f,
    val prevEpScale: Float = 1.0f,

    val nextEpDeltaX: Float = 0f,
    val nextEpDeltaY: Float = 0f,
    val nextEpScale: Float = 1.0f,

    val topBarDeltaX: Float = 0f,
    val topBarDeltaY: Float = 0f,
    val topBarScale: Float = 1.0f,

    val seekbarDeltaX: Float = 0f,
    val seekbarDeltaY: Float = 0f,
    val seekbarScale: Float = 1.0f
)

data class ElementPlacementConfig(
    val lockAnchor: LockScreenAnchor = LockScreenAnchor.TOP_LEFT,
    val lockCustomXFraction: Float = 0.03f,
    val lockCustomYFraction: Float = 0.05f,
    val centerLayout: CenterControlsLayout = CenterControlsLayout.STANDARD_5,
    val centerScale: Float = 1.0f,
    val centerOffsetY: Float = 0f,
    val showEpisodeSubtitle: Boolean = true,
    val seekbarHeightDp: Float = 3.5f
)

data class GlassBlurConfig(
    val overlayOpacity: Float = 0.85f,
    val glassBlurStrength: Float = 12f,
    val borderShineAlpha: Float = 0.30f,
    val controlGlowStrength: Float = 0.65f,
    val autoHideTimeoutSeconds: Int = 4
)

data class ButtonIconCustomization(
    val iconPack: IconPackPreset = IconPackPreset.MODERN_MINIMAL,
    val playStyle: PlayIconStyle = PlayIconStyle.DEFAULT,
    val seekStyle: SeekIconStyle = SeekIconStyle.DEFAULT,
    val lockStyle: LockIconStyle = LockIconStyle.SLEEK_PADLOCK,
    val subtitleStyle: SubtitleIconStyle = SubtitleIconStyle.CLOSED_CAPTION,
    val audioStyle: AudioIconStyle = AudioIconStyle.MUSIC_NOTE,
    val settingsStyle: SettingsIconStyle = SettingsIconStyle.GEAR
)

data class PlayerGestureSettings(
    val masterEnabled: Boolean = true,
    val volumeEnabled: Boolean = true,
    val brightnessEnabled: Boolean = true,
    val swipeSeekEnabled: Boolean = true,
    val holdTo2xEnabled: Boolean = true,
    val doubleTapSeekEnabled: Boolean = true,
    val pinchToZoomEnabled: Boolean = true
)

data class CinePlayerCustomizationSettings(
    val themeId: PlayerThemeId = PlayerThemeId.ZENKAI_DEFAULT,
    val themePack: PlayerThemePack = PlayerThemePack.ZENKAI_CLASSIC,
    val accentColor: PlayerAccentColor = PlayerAccentColor.PURE_WHITE,
    val loadingStyle: LoadingBarStyle = LoadingBarStyle.ZENKAI_FLAME,
    val seekbarStyle: SeekbarStyle = SeekbarStyle.GLOW_CAPSULE,
    val icons: ButtonIconCustomization = ButtonIconCustomization(),
    val hudLayout: HudLayoutConfig = HudLayoutConfig(),
    val placement: ElementPlacementConfig = ElementPlacementConfig(),
    val glass: GlassBlurConfig = GlassBlurConfig(),
    val gestures: PlayerGestureSettings = PlayerGestureSettings()
) {
    fun toJson(): String = try {
        val obj = JSONObject().apply {
            put("themePack", themePack.name)
            put("accentColor", accentColor.name)
            put("loadingStyle", loadingStyle.name)
            put("seekbarStyle", seekbarStyle.name)

            val iconsObj = JSONObject().apply {
                put("iconPack", icons.iconPack.name)
                put("playStyle", icons.playStyle.name)
                put("seekStyle", icons.seekStyle.name)
                put("lockStyle", icons.lockStyle.name)
                put("subtitleStyle", icons.subtitleStyle.name)
                put("audioStyle", icons.audioStyle.name)
                put("settingsStyle", icons.settingsStyle.name)
            }
            put("icons", iconsObj)

            val hudObj = JSONObject().apply {
                put("lockDeltaX", hudLayout.lockDeltaX.toDouble())
                put("lockDeltaY", hudLayout.lockDeltaY.toDouble())
                put("lockScale", hudLayout.lockScale.toDouble())
                put("playPauseDeltaX", hudLayout.playPauseDeltaX.toDouble())
                put("playPauseDeltaY", hudLayout.playPauseDeltaY.toDouble())
                put("playPauseScale", hudLayout.playPauseScale.toDouble())
                put("rewindDeltaX", hudLayout.rewindDeltaX.toDouble())
                put("rewindDeltaY", hudLayout.rewindDeltaY.toDouble())
                put("rewindScale", hudLayout.rewindScale.toDouble())
                put("forwardDeltaX", hudLayout.forwardDeltaX.toDouble())
                put("forwardDeltaY", hudLayout.forwardDeltaY.toDouble())
                put("forwardScale", hudLayout.forwardScale.toDouble())
                put("prevEpDeltaX", hudLayout.prevEpDeltaX.toDouble())
                put("prevEpDeltaY", hudLayout.prevEpDeltaY.toDouble())
                put("prevEpScale", hudLayout.prevEpScale.toDouble())
                put("nextEpDeltaX", hudLayout.nextEpDeltaX.toDouble())
                put("nextEpDeltaY", hudLayout.nextEpDeltaY.toDouble())
                put("nextEpScale", hudLayout.nextEpScale.toDouble())
                put("topBarDeltaX", hudLayout.topBarDeltaX.toDouble())
                put("topBarDeltaY", hudLayout.topBarDeltaY.toDouble())
                put("topBarScale", hudLayout.topBarScale.toDouble())
                put("seekbarDeltaX", hudLayout.seekbarDeltaX.toDouble())
                put("seekbarDeltaY", hudLayout.seekbarDeltaY.toDouble())
                put("seekbarScale", hudLayout.seekbarScale.toDouble())
            }
            put("hudLayout", hudObj)

            val placeObj = JSONObject().apply {
                put("lockAnchor", placement.lockAnchor.name)
                put("lockCustomXFraction", placement.lockCustomXFraction.toDouble())
                put("lockCustomYFraction", placement.lockCustomYFraction.toDouble())
                put("centerLayout", placement.centerLayout.name)
                put("centerScale", placement.centerScale.toDouble())
                put("centerOffsetY", placement.centerOffsetY.toDouble())
                put("showEpisodeSubtitle", placement.showEpisodeSubtitle)
                put("seekbarHeightDp", placement.seekbarHeightDp.toDouble())
            }
            put("placement", placeObj)

            val glassObj = JSONObject().apply {
                put("overlayOpacity", glass.overlayOpacity.toDouble())
                put("glassBlurStrength", glass.glassBlurStrength.toDouble())
                put("borderShineAlpha", glass.borderShineAlpha.toDouble())
                put("controlGlowStrength", glass.controlGlowStrength.toDouble())
                put("autoHideTimeoutSeconds", glass.autoHideTimeoutSeconds)
            }
            put("glass", glassObj)

            val gesturesObj = JSONObject().apply {
                put("masterEnabled", gestures.masterEnabled)
                put("volumeEnabled", gestures.volumeEnabled)
                put("brightnessEnabled", gestures.brightnessEnabled)
                put("swipeSeekEnabled", gestures.swipeSeekEnabled)
                put("holdTo2xEnabled", gestures.holdTo2xEnabled)
                put("doubleTapSeekEnabled", gestures.doubleTapSeekEnabled)
                put("pinchToZoomEnabled", gestures.pinchToZoomEnabled)
            }
            put("gestures", gesturesObj)
        }
        obj.toString()
    } catch (_: Exception) {
        ""
    }

    companion object {
        fun fromJson(jsonStr: String?): CinePlayerCustomizationSettings {
            if (jsonStr.isNullOrBlank()) return CinePlayerCustomizationSettings()
            return try {
                val obj = JSONObject(jsonStr)
                val themePack = runCatching { PlayerThemePack.valueOf(obj.optString("themePack", PlayerThemePack.ZENKAI_CLASSIC.name)) }.getOrDefault(PlayerThemePack.ZENKAI_CLASSIC)
                val accentColor = runCatching { PlayerAccentColor.valueOf(obj.optString("accentColor", PlayerAccentColor.PURE_WHITE.name)) }.getOrDefault(PlayerAccentColor.PURE_WHITE)
                val loadingStyle = runCatching { LoadingBarStyle.valueOf(obj.optString("loadingStyle", themePack.loadingStyle.name)) }.getOrDefault(themePack.loadingStyle)
                val seekbarStyle = runCatching { SeekbarStyle.valueOf(obj.optString("seekbarStyle", themePack.seekbarStyle.name)) }.getOrDefault(themePack.seekbarStyle)

                val iconsObj = obj.optJSONObject("icons")
                val icons = if (iconsObj != null) {
                    ButtonIconCustomization(
                        iconPack = runCatching { IconPackPreset.valueOf(iconsObj.optString("iconPack", themePack.iconPack.name)) }.getOrDefault(themePack.iconPack),
                        playStyle = runCatching { PlayIconStyle.valueOf(iconsObj.optString("playStyle", themePack.playStyle.name)) }.getOrDefault(themePack.playStyle),
                        seekStyle = runCatching { SeekIconStyle.valueOf(iconsObj.optString("seekStyle", themePack.seekStyle.name)) }.getOrDefault(themePack.seekStyle),
                        lockStyle = runCatching { LockIconStyle.valueOf(iconsObj.optString("lockStyle", themePack.lockStyle.name)) }.getOrDefault(themePack.lockStyle),
                        subtitleStyle = runCatching { SubtitleIconStyle.valueOf(iconsObj.optString("subtitleStyle", SubtitleIconStyle.CLOSED_CAPTION.name)) }.getOrDefault(SubtitleIconStyle.CLOSED_CAPTION),
                        audioStyle = runCatching { AudioIconStyle.valueOf(iconsObj.optString("audioStyle", AudioIconStyle.MUSIC_NOTE.name)) }.getOrDefault(AudioIconStyle.MUSIC_NOTE),
                        settingsStyle = runCatching { SettingsIconStyle.valueOf(iconsObj.optString("settingsStyle", SettingsIconStyle.GEAR.name)) }.getOrDefault(SettingsIconStyle.GEAR)
                    )
                } else ButtonIconCustomization(
                    iconPack = themePack.iconPack,
                    playStyle = themePack.playStyle,
                    seekStyle = themePack.seekStyle,
                    lockStyle = themePack.lockStyle
                )

                val hudObj = obj.optJSONObject("hudLayout")
                val hudLayout = if (hudObj != null) {
                    HudLayoutConfig(
                        lockDeltaX = hudObj.optDouble("lockDeltaX", 0.0).toFloat(),
                        lockDeltaY = hudObj.optDouble("lockDeltaY", 0.0).toFloat(),
                        lockScale = hudObj.optDouble("lockScale", 1.0).toFloat(),
                        playPauseDeltaX = hudObj.optDouble("playPauseDeltaX", 0.0).toFloat(),
                        playPauseDeltaY = hudObj.optDouble("playPauseDeltaY", 0.0).toFloat(),
                        playPauseScale = hudObj.optDouble("playPauseScale", 1.0).toFloat(),
                        rewindDeltaX = hudObj.optDouble("rewindDeltaX", 0.0).toFloat(),
                        rewindDeltaY = hudObj.optDouble("rewindDeltaY", 0.0).toFloat(),
                        rewindScale = hudObj.optDouble("rewindScale", 1.0).toFloat(),
                        forwardDeltaX = hudObj.optDouble("forwardDeltaX", 0.0).toFloat(),
                        forwardDeltaY = hudObj.optDouble("forwardDeltaY", 0.0).toFloat(),
                        forwardScale = hudObj.optDouble("forwardScale", 1.0).toFloat(),
                        prevEpDeltaX = hudObj.optDouble("prevEpDeltaX", 0.0).toFloat(),
                        prevEpDeltaY = hudObj.optDouble("prevEpDeltaY", 0.0).toFloat(),
                        prevEpScale = hudObj.optDouble("prevEpScale", 1.0).toFloat(),
                        nextEpDeltaX = hudObj.optDouble("nextEpDeltaX", 0.0).toFloat(),
                        nextEpDeltaY = hudObj.optDouble("nextEpDeltaY", 0.0).toFloat(),
                        nextEpScale = hudObj.optDouble("nextEpScale", 1.0).toFloat(),
                        topBarDeltaX = hudObj.optDouble("topBarDeltaX", 0.0).toFloat(),
                        topBarDeltaY = hudObj.optDouble("topBarDeltaY", 0.0).toFloat(),
                        topBarScale = hudObj.optDouble("topBarScale", 1.0).toFloat(),
                        seekbarDeltaX = hudObj.optDouble("seekbarDeltaX", 0.0).toFloat(),
                        seekbarDeltaY = hudObj.optDouble("seekbarDeltaY", 0.0).toFloat(),
                        seekbarScale = hudObj.optDouble("seekbarScale", 1.0).toFloat()
                    )
                } else HudLayoutConfig()

                val placeObj = obj.optJSONObject("placement")
                val placement = if (placeObj != null) {
                    ElementPlacementConfig(
                        lockAnchor = runCatching { LockScreenAnchor.valueOf(placeObj.optString("lockAnchor", LockScreenAnchor.TOP_LEFT.name)) }.getOrDefault(LockScreenAnchor.TOP_LEFT),
                        lockCustomXFraction = placeObj.optDouble("lockCustomXFraction", 0.03).toFloat(),
                        lockCustomYFraction = placeObj.optDouble("lockCustomYFraction", 0.05).toFloat(),
                        centerLayout = runCatching { CenterControlsLayout.valueOf(placeObj.optString("centerLayout", CenterControlsLayout.STANDARD_5.name)) }.getOrDefault(CenterControlsLayout.STANDARD_5),
                        centerScale = placeObj.optDouble("centerScale", 1.0).toFloat(),
                        centerOffsetY = placeObj.optDouble("centerOffsetY", 0.0).toFloat(),
                        showEpisodeSubtitle = placeObj.optBoolean("showEpisodeSubtitle", true),
                        seekbarHeightDp = placeObj.optDouble("seekbarHeightDp", 3.5).toFloat()
                    )
                } else ElementPlacementConfig()

                val glassObj = obj.optJSONObject("glass")
                val glass = if (glassObj != null) {
                    GlassBlurConfig(
                        overlayOpacity = glassObj.optDouble("overlayOpacity", 0.85).toFloat(),
                        glassBlurStrength = glassObj.optDouble("glassBlurStrength", 12.0).toFloat(),
                        borderShineAlpha = glassObj.optDouble("borderShineAlpha", 0.30).toFloat(),
                        controlGlowStrength = glassObj.optDouble("controlGlowStrength", 0.65).toFloat(),
                        autoHideTimeoutSeconds = glassObj.optInt("autoHideTimeoutSeconds", 4)
                    )
                } else GlassBlurConfig()

                val gesturesObj = obj.optJSONObject("gestures")
                val gestures = if (gesturesObj != null) {
                    PlayerGestureSettings(
                        masterEnabled = gesturesObj.optBoolean("masterEnabled", true),
                        volumeEnabled = gesturesObj.optBoolean("volumeEnabled", true),
                        brightnessEnabled = gesturesObj.optBoolean("brightnessEnabled", true),
                        swipeSeekEnabled = gesturesObj.optBoolean("swipeSeekEnabled", true),
                        holdTo2xEnabled = gesturesObj.optBoolean("holdTo2xEnabled", true),
                        doubleTapSeekEnabled = gesturesObj.optBoolean("doubleTapSeekEnabled", true),
                        pinchToZoomEnabled = gesturesObj.optBoolean("pinchToZoomEnabled", true)
                    )
                } else PlayerGestureSettings()

                CinePlayerCustomizationSettings(
                    themePack = themePack,
                    accentColor = accentColor,
                    loadingStyle = loadingStyle,
                    seekbarStyle = seekbarStyle,
                    icons = icons,
                    hudLayout = hudLayout,
                    placement = placement,
                    glass = glass,
                    gestures = gestures
                )
            } catch (_: Exception) {
                CinePlayerCustomizationSettings()
            }
        }
    }
}

// ==========================================
// 2. CLEAN WHITE LOADING BAR RENDERER
// ==========================================

@Composable
fun CineLoadingBarRenderer(
    style: LoadingBarStyle,
    palette: PlayerThemePalette = PlayerThemePalette.CLEAN_ZENKAI,
    accentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cineLoading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinRot"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterSpinRot"
    )
    val dynamicSweep by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dynamicSweep"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LoadingBarStyle.ZENKAI_FLAME -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 3.2.dp.toPx()
                    val centerPt = Offset(this.size.width / 2f, this.size.height / 2f)
                    val r = (this.size.minDimension / 2f) - stroke

                    // Subtle background orbit track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = r,
                        center = centerPt,
                        style = Stroke(width = stroke * 0.7f)
                    )

                    // Outer primary glowing energy arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                accentColor.copy(alpha = 0.05f),
                                accentColor.copy(alpha = 0.40f),
                                accentColor.copy(alpha = 0.95f),
                                Color.White
                            )
                        ),
                        startAngle = rotation,
                        sweepAngle = dynamicSweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Inner counter-orbiting micro-arc
                    val innerStroke = 1.8.dp.toPx()
                    val innerR = r * 0.62f
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                accentColor.copy(alpha = 0.10f),
                                accentColor.copy(alpha = 0.85f),
                                Color.White
                            )
                        ),
                        startAngle = counterRotation,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(centerPt.x - innerR, centerPt.y - innerR),
                        size = androidx.compose.ui.geometry.Size(innerR * 2, innerR * 2),
                        style = Stroke(width = innerStroke, cap = StrokeCap.Round)
                    )
                }
                // Glowing pulsating center core
                Box(
                    modifier = Modifier
                        .size(size * 0.22f)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            LoadingBarStyle.NEON_PULSE -> {
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                    val stroke = 3.dp.toPx()
                    val r = (this.size.minDimension / 2f) - stroke
                    drawCircle(
                        color = accentColor.copy(alpha = 0.12f),
                        radius = r,
                        style = Stroke(width = stroke * 0.6f)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, accentColor.copy(alpha = 0.4f), accentColor)),
                        startAngle = 0f,
                        sweepAngle = 130f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, accentColor.copy(alpha = 0.4f), accentColor)),
                        startAngle = 180f,
                        sweepAngle = 130f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(size * 0.26f)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.85f))
                )
            }

            LoadingBarStyle.CYBER_WAVE -> {
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                    val stroke = 2.5.dp.toPx()
                    for (i in 0 until 4) {
                        drawArc(
                            color = if (i % 2 == 0) accentColor else accentColor.copy(alpha = 0.35f),
                            startAngle = i * 90f + 12f,
                            sweepAngle = 66f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(size * 0.20f)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.60f))
                )
            }

            LoadingBarStyle.MINIMAL_ARC -> {
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                    val stroke = 2.8.dp.toPx()
                    val r = (this.size.minDimension / 2f) - stroke
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = r,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = 0f,
                        sweepAngle = dynamicSweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            LoadingBarStyle.QUANTUM_DOTS -> {
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                    val centerOffset = Offset(this.size.width / 2, this.size.height / 2)
                    val orbitRadius = this.size.minDimension / 2.6f
                    val dotRadius = 4.dp.toPx()

                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = orbitRadius,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    for (i in 0 until 3) {
                        val angle = (i * 120f) * (PI / 180f)
                        val x = centerOffset.x + (orbitRadius * cos(angle)).toFloat()
                        val y = centerOffset.y + (orbitRadius * sin(angle)).toFloat()
                        drawCircle(
                            color = if (i == 0) accentColor else accentColor.copy(alpha = 0.45f),
                            radius = if (i == 0) dotRadius * 1.25f else dotRadius,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. CLEAN WHITE SEEKBAR RENDERER
// ==========================================

@Composable
fun CineSeekbarRenderer(
    style: SeekbarStyle,
    palette: PlayerThemePalette = PlayerThemePalette.CLEAN_ZENKAI,
    accentColor: Color = Color.White,
    playedFrac: Float,
    bufferedFrac: Float,
    isScrubbing: Boolean,
    heightDp: Float = 3.5f,
    modifier: Modifier = Modifier
) {
    val clampedPlayed = playedFrac.coerceIn(0f, 1f)
    val clampedBuffered = bufferedFrac.coerceIn(0f, 1f)

    val pressProgress by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "seekbarPress"
    )

    // Track dynamic drag velocity to stretch and squash the thumb in the swipe direction
    var prevPlayed by remember { mutableFloatStateOf(clampedPlayed) }
    var scrubVelocity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(clampedPlayed, isScrubbing) {
        if (isScrubbing) {
            val delta = clampedPlayed - prevPlayed
            prevPlayed = clampedPlayed
            scrubVelocity = (delta * 140f).coerceIn(-1.5f, 1.5f)
        } else {
            prevPlayed = clampedPlayed
            scrubVelocity = 0f
        }
    }
    val animatedVelocity by animateFloatAsState(
        targetValue = if (isScrubbing) scrubVelocity else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scrubVelocity"
    )

    val stretchFactor = (kotlin.math.abs(animatedVelocity) * 0.45f).coerceIn(0f, 0.6f)
    val thumbScaleX = lerp(1f, 1.45f, pressProgress) * (1f + stretchFactor)
    val thumbScaleY = lerp(1f, 1.45f, pressProgress) / (1f + stretchFactor * 0.6f)

    val barHeight = (heightDp + (1.5f * pressProgress)).dp
    val thumbSize = 13.dp

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val maxOffsetPx = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)
    val thumbOffsetXPx = (maxOffsetPx * clampedPlayed).roundToInt()

    val prismalBackdrop = LocalPrismalBackdrop.current
    val isGlassEnabled = LocalThemeSettings.current.isGlassmorphismEnabled

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { size ->
                if (size.width > 0) trackWidthPx = size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        when (style) {
            SeekbarStyle.GLOW_CAPSULE -> {
                // Background track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x35FFFFFF))
                ) {
                    if (clampedBuffered > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedBuffered)
                                .background(Color(0x55FFFFFF))
                        )
                    }
                    if (clampedPlayed > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedPlayed)
                                .background(accentColor.copy(alpha = 0.90f))
                        )
                    }
                }
                // Thumb Head with velocity squash/stretch and live video refraction
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetXPx, 0) }
                        .size(thumbSize)
                        .graphicsLayer {
                            scaleX = thumbScaleX
                            scaleY = thumbScaleY
                        }
                        .then(
                            if (isGlassEnabled && prismalBackdrop != null) {
                                Modifier.drawPrismalGlass(
                                    backdrop = prismalBackdrop,
                                    shape = { CircleShape },
                                    effects = {
                                        applyPrismalGlassEffects(
                                            density = density,
                                            adaptiveLuminance = false,
                                            luminance = 0.5f,
                                            blurRadiusPx = with(density) { 14.dp.toPx() } * (1f - pressProgress * 0.5f),
                                            refractionHeightPx = with(density) { 12.dp.toPx() } * pressProgress,
                                            refractionAmountPx = with(density) { 18.dp.toPx() } * pressProgress,
                                            chromaticAberration = 1f,
                                            useVibrancy = true,
                                            saturation = 1.35f
                                        )
                                    },
                                    specular = {
                                        PrismalSpecular.Default.copy(
                                            alpha = 0.75f + (0.25f * pressProgress)
                                        )
                                    },
                                    depthShadow = {
                                        PrismalDepthShadow(radius = 6.dp * pressProgress, alpha = 0.8f)
                                    },
                                    depthInset = {
                                        PrismalDepthInset(radius = 4.dp * pressProgress, alpha = pressProgress)
                                    },
                                    onDrawSurface = {
                                        val solidAlpha = (1f - pressProgress * 0.75f).coerceIn(0.18f, 1f)
                                        drawRect(accentColor.copy(alpha = solidAlpha))
                                    }
                                )
                            } else {
                                Modifier
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            }
                        )
                        .border(
                            1.2.dp,
                            if (pressProgress > 0.1f) Color(0x75FFFFFF) else accentColor.copy(alpha = 0.85f),
                            CircleShape
                        )
                )
            }

            SeekbarStyle.MINIMAL_NEON -> {
                // Razor thin laser line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(Color(0x25FFFFFF))
                ) {
                    if (clampedBuffered > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedBuffered)
                                .background(Color(0x40FFFFFF))
                        )
                    }
                    if (clampedPlayed > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedPlayed)
                                .background(accentColor.copy(alpha = 0.90f))
                        )
                    }
                }
                val neonThumbPx = with(density) { 8.dp.toPx() }
                val neonOffset = ((trackWidthPx - neonThumbPx).coerceAtLeast(0f) * clampedPlayed).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(neonOffset, 0) }
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = thumbScaleX
                            scaleY = thumbScaleY
                        }
                        .clip(CircleShape)
                        .background(accentColor)
                        .border(1.dp, accentColor.copy(alpha = 0.70f), CircleShape)
                )
            }

            SeekbarStyle.CHUNKY_GLASS -> {
                // Modern frosted pill bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight + 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x30FFFFFF))
                        .border(0.5.dp, Color(0x20FFFFFF), RoundedCornerShape(4.dp))
                ) {
                    if (clampedBuffered > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedBuffered)
                                .background(Color(0x50FFFFFF))
                        )
                    }
                    if (clampedPlayed > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedPlayed)
                                .background(accentColor.copy(alpha = 0.90f))
                        )
                    }
                }
                val chunkyThumbPx = with(density) { 14.dp.toPx() }
                val chunkyOffset = ((trackWidthPx - chunkyThumbPx).coerceAtLeast(0f) * clampedPlayed).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(chunkyOffset, 0) }
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = thumbScaleX
                            scaleY = thumbScaleY
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                        .border(1.5.dp, accentColor.copy(alpha = 0.70f), RoundedCornerShape(4.dp))
                )
            }

            SeekbarStyle.CYBER_SEGMENTS -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight + 4.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    if (w > 0f && h > 0f) {
                        drawRect(color = Color(0x25FFFFFF), size = Size(w, h))
                        if (clampedBuffered > 0f) {
                            drawRect(color = Color(0x45FFFFFF), size = Size(w * clampedBuffered, h))
                        }
                        if (clampedPlayed > 0f) {
                            drawRect(
                                color = accentColor.copy(alpha = 0.90f),
                                size = Size(w * clampedPlayed, h)
                            )
                        }
                        val step = w / 20f
                        for (i in 1..19) {
                            val x = i * step
                            drawLine(
                                color = Color(0x55000000),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                }
            }

            SeekbarStyle.WAVE_ENERGY -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight + 1.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x25FFFFFF))
                ) {
                    if (clampedBuffered > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedBuffered)
                                .background(Color(0x45FFFFFF))
                        )
                    }
                    if (clampedPlayed > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedPlayed)
                                .background(accentColor.copy(alpha = 0.90f))
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetXPx, 0) }
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(accentColor)
                        .border(1.5.dp, accentColor.copy(alpha = 0.70f), CircleShape)
                )
            }
        }
    }
}

// ==========================================
// 4. iOS CIRCULAR ARROW SEEK 10 ICON
// ==========================================

// ==========================================
// 4. iOS CIRCULAR ARROW SEEK 10 ICON
// ==========================================

@Composable
fun IosSeek10Icon(
    isForward: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    fontSize: TextUnit = 11.sp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.minDimension <= 0f) return@Canvas
            val strokeWidth = 1.9.dp.toPx()
            val diameter = size.minDimension * 0.86f
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Gap at top of arc: ~55 degrees opening
            val startAngle = if (isForward) -65f else -115f
            val sweepAngle = if (isForward) 305f else -305f

            drawArc(
                color = tint,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Triangular arrowhead at the arc head
            val angleRad = Math.toRadians((if (isForward) -65.0 else -115.0)).toFloat()
            val tipX = center.x + radius * cos(angleRad)
            val tipY = center.y + radius * sin(angleRad)
            val arrowSize = strokeWidth * 2.2f

            val arrowPath = Path().apply {
                if (isForward) {
                    moveTo(tipX - arrowSize * 0.35f, tipY - arrowSize * 1.0f)
                    lineTo(tipX + arrowSize * 0.95f, tipY)
                    lineTo(tipX - arrowSize * 0.95f, tipY + arrowSize * 0.75f)
                } else {
                    moveTo(tipX + arrowSize * 0.35f, tipY - arrowSize * 1.0f)
                    lineTo(tipX - arrowSize * 0.95f, tipY)
                    lineTo(tipX + arrowSize * 0.95f, tipY + arrowSize * 0.75f)
                }
                close()
            }
            drawPath(arrowPath, color = tint)
        }

        // Crisp bold "10" in the dead center
        Text(
            text = "10",
            color = tint,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// 5. iOS DEPTH GLASSMORPHISM — BUTTON CONTAINER
// ==========================================

// Directional rim lighting: bright specular top-left corner, soft ambient bottom
private val GlassBorderColors = listOf(
    Color(0x95FFFFFF), // crisp specular highlight top-left
    Color(0x35FFFFFF), // soft falloff
    Color(0x10FFFFFF), // shadow side
    Color(0x28FFFFFF)  // bounce rim light bottom-right
)

// Water-like rainbow iridescence upon press/touch
private val GlassRainbowColors = listOf(
    Color(0x18FF5E7E), Color(0x18FFAA44), Color(0x18FFED47),
    Color(0x1852E080), Color(0x1842C5FF), Color(0x18A855F7),
    Color(0x18FF5E7E)
)
private val GlassRainbowColorsPressed = listOf(
    Color(0x75FF5E7E), Color(0x75FFAA44), Color(0x75FFED47),
    Color(0x7552E080), Color(0x7542C5FF), Color(0x75A855F7),
    Color(0x75FF5E7E)
)

// ==========================================
// iOS 26 LIQUID-GLASS SURFACE (shared by buttons, pills, panels)
// ==========================================

/** Single accent for tinted glass (swap here to re-theme). */
val GlassAccent = Color(0xFF0A84FF)

private val GlassRainbowRim = listOf(
    Color(0x66FFFFFF), Color(0x26FF6B9D), Color(0x26FFB347), Color(0x2652E080),
    Color(0x2642C5FF), Color(0x26A78BFA), Color(0x66FFFFFF)
)

/** Live, tiny snapshot of the video used as real frosted backdrop behind every glass surface. */
class GlassBackdrop {
    var bitmap by mutableStateOf<ImageBitmap?>(null)
    var origin by mutableStateOf(Offset.Zero)     // window position of the video surface
    var size by mutableStateOf(IntSize.Zero)      // video surface size in px
    var hostWidth by mutableStateOf(0)            // player container width in px (for compact mode)
}

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }
val LocalGlassCompact = staticCompositionLocalOf { false }
/** Non-null only on Android 12+ (API 31) where the video is a TextureView and Haze can blur it. */
val LocalGlassHaze = staticCompositionLocalOf<HazeState?> { null }

/** No-op: Android 12+ uses Haze (live backdrop blur on a TextureView); older versions use plain translucent glass. */
@Composable
fun GlassBackdropCapture(backdrop: GlassBackdrop, playerView: PlayerView?, enabled: Boolean, isPlaying: Boolean = false) {}

/**
 * Clean frosted glass matching the Zenkai Web CinePlayer UI:
 * Translucent glass (rgba(255, 255, 255, 0.16) for standard, 0.24 for hero play button),
 * crisp border (rgba(255, 255, 255, 0.22)), top-lit shine, and subtle depth shadow.
 * Fully static Compose drawing for buttery 120 FPS performance with ZERO flickering.
 */
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color = Color(0x29FFFFFF), // rgba(255, 255, 255, 0.16) matching web .cine-icon-circle-btn & .cine-center-btn
    wash: Color = Color.Transparent,
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color(0x38FFFFFF), // rgba(255, 255, 255, 0.22)
    useBackdrop: Boolean = true
): Modifier = composed {
    val isGlassEnabled = com.styropyr0.prismal.PrismalGlass.isGlassmorphismEnabled
    if (!isGlassEnabled) {
        return@composed this
            .shadow(if (elevation > 0.dp) 2.dp else 0.dp, shape, clip = false)
            .clip(shape)
            .background(Color(0xE6101420))
            .drawBehind { if (pressed) drawRect(Color(0x33FFFFFF)) }
            .border(borderWidth, Color(0x28FFFFFF), shape)
    }
    val prismalBackdrop = LocalPrismalBackdrop.current
    if (useBackdrop && prismalBackdrop != null) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        return@composed this.drawPrismalGlass(
            backdrop = prismalBackdrop,
            shape = { shape },
            effects = {
                applyPrismalGlassEffects(
                    density = density,
                    adaptiveLuminance = false,
                    luminance = 0.5f,
                    blurRadiusPx = with(density) { 20.dp.toPx() },
                    refractionHeightPx = with(density) { 14.dp.toPx() },
                    refractionAmountPx = with(density) { 20.dp.toPx() },
                    useVibrancy = true,
                    saturation = 1.3f
                )
            },
            specular = {
                PrismalSpecular(
                    width = 0.8.dp,
                    blurRadius = 0.5.dp,
                    alpha = if (pressed) 1.0f else 0.75f,
                    style = PrismalSpecularStyle.Default
                )
            },
            depthShadow = {
                PrismalDepthShadow(radius = elevation, alpha = 0.8f)
            },
            onDrawSurface = {
                drawRect(tint)
                if (wash != Color.Transparent) drawRect(wash)
                if (pressed) drawRect(Color(0x33FFFFFF))
            }
        )
    }
    // Android 12+: real live backdrop blur of the video (Haze / AGSL). Below 12: plain translucent glass.
    val haze = if (useBackdrop) LocalGlassHaze.current else null
    this
        .shadow(elevation, shape, clip = false, ambientColor = Color(0x59000000), spotColor = Color(0x59000000))
        .clip(shape)
        .then(
            if (haze != null) Modifier.hazeEffect(haze) {
                blurRadius = 28.dp
                tints = listOf(HazeTint(tint))
                noiseFactor = 0.04f
            } else Modifier.background(tint)
        )
        .background(wash)
        .background(Brush.verticalGradient(listOf(Color(0x24FFFFFF), Color(0x0AFFFFFF), Color(0x0A000000))))
        .drawBehind { if (pressed) drawRect(Color(0x33FFFFFF)) }
        .border(borderWidth, Brush.verticalGradient(listOf(borderColor.copy(alpha = 0.55f), borderColor)), shape)
}

@Composable
fun CineButtonContainer(
    iconPack: IconPackPreset = IconPackPreset.MODERN_MINIMAL,
    palette: PlayerThemePalette = PlayerThemePalette.CLEAN_ZENKAI,
    accentColor: Color = Color.White,
    isSelected: Boolean = false,
    isHero: Boolean = false,
    isCircle: Boolean = true,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by src.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glassPress"
    )
    val shape = when {
        !isCircle -> RoundedCornerShape(12.dp)
        iconPack == IconPackPreset.CYBER_NEON ||
        iconPack == IconPackPreset.BLADE_SAMURAI ||
        iconPack == IconPackPreset.RETRO_ARCADE -> RoundedCornerShape(10.dp)
        else -> CircleShape
    }
    Box(
        modifier = modifier
            .scale(pressScale)
            .glassSurface(
                shape = shape,
                tint = if (isHero) Color(0x3DFFFFFF) else Color(0x29FFFFFF), // 0.24 for hero play, 0.16 for other buttons
                wash = if (isHero) Color(0x1AFFFFFF) else Color.Transparent,
                elevation = if (isHero) 10.dp else 5.dp,
                pressed = isPressed,
                borderWidth = if (isHero) 1.5.dp else 1.dp,
                borderColor = if (isHero) Color(0x73FFFFFF) else Color(0x38FFFFFF)
            )
            .then(if (isSelected && !isHero) Modifier.background(accentColor.copy(alpha = 0.25f)) else Modifier),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Small glass round button with a larger invisible touch area. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    enabled: Boolean = true,
    isHero: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val src = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size + 12.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = src,
                indication = null,
                enabled = enabled,
                onClick = {
                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        CineButtonContainer(isHero = isHero, interactionSource = src, modifier = Modifier.size(size), content = content)
    }
}

/** Bare icon in a GlassPill. onClick receives the icon's window bounds so a panel can morph out of it. */
@Composable
fun GlassPillIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xE6FFFFFF),
    iconSize: Dp = 17.dp
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val compact = LocalGlassCompact.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.78f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pillIconPress"
    )
    Box(
        modifier = modifier
            .size(if (compact) 27.dp else 32.dp)
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .scale(s)
            .clickable(interactionSource = src, indication = null) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick(bounds)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(if (compact) iconSize * 0.88f else iconSize))
    }
}

/** Rounded play / pause glyphs matching the iOS reference. */
@Composable
fun GlassPlayPauseGlyph(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            val barW = w * 0.25f
            val gap = w * 0.17f
            val left = (w - 2 * barW - gap) / 2f
            val r = CornerRadius(barW * 0.4f)
            drawRoundRect(tint, Offset(left, h * 0.1f), Size(barW, h * 0.8f), r)
            drawRoundRect(tint, Offset(left + barW + gap, h * 0.1f), Size(barW, h * 0.8f), r)
        } else {
            val p = Path().apply {
                moveTo(w * 0.28f, h * 0.14f)
                lineTo(w * 0.88f, h * 0.5f)
                lineTo(w * 0.28f, h * 0.86f)
                close()
            }
            drawPath(p, tint)
            drawPath(p, tint, style = Stroke(width = w * 0.12f, join = StrokeJoin.Round))
        }
    }
}


// ==========================================
// 6. GLASS PILL — capsule container (icons inside are bare, never nested glass)
// ==========================================

@Composable
fun GlassPill(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pillPressScale"
    )
    Box(
        modifier = modifier
            .scale(pressScale)
            .glassSurface(shape, pressed = isPressed)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick) else Modifier
            )
            .padding(horizontal = if (LocalGlassCompact.current) 5.dp else 8.dp, vertical = if (LocalGlassCompact.current) 2.dp else 3.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ==========================================
// 7. GLASS DIALOG — panel that morphs out of the tapped button, with real blur-behind (Android 12+)
// ==========================================

@Composable
fun GlassDialog(
    anchor: Rect?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    onDismissRequest: (close: () -> Unit) -> Unit = { it() },
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var origin by remember { mutableStateOf<TransformOrigin?>(null) }
    LaunchedEffect(Unit) { progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 520f)) }
    val close: () -> Unit = { scope.launch { progress.animateTo(0f, tween(140, easing = FastOutSlowInEasing)); onDismiss() } }

    Dialog(
        onDismissRequest = { onDismissRequest(close) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val blur = rememberCrossWindowBlur()
        SideEffect {
            window?.let { w ->
                w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                w.setDimAmount(if (blur) 0.18f else 0.5f)
                if (blur && Build.VERSION.SDK_INT >= 31) {
                    w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    w.attributes = w.attributes.apply { blurBehindRadius = 80 }
                }
            }
        }
        val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
        val currentClose by rememberUpdatedState(close)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { currentOnDismissRequest(currentClose) } }
        ) {
            val fromTop = anchor == null || anchor.center.y < constraints.maxHeight / 2f
            val fromStart = anchor == null || anchor.center.x < constraints.maxWidth / 2f
            val align = when {
                centered -> Alignment.Center
                fromTop && fromStart -> Alignment.TopStart
                fromTop -> Alignment.TopEnd
                fromStart -> Alignment.BottomStart
                else -> Alignment.BottomEnd
            }
            val p = progress.value
            val d = LocalDensity.current
            val isLandscape = constraints.maxWidth > constraints.maxHeight
            val edgePad = if (isLandscape) 10.dp else 24.dp
            val fallbackPad = if (isLandscape) 14.dp else 62.dp

            val topPad = when {
                centered -> 0.dp
                anchor != null && fromTop -> with(d) { anchor.bottom.toDp() } + 6.dp
                anchor != null && !fromTop -> edgePad
                else -> fallbackPad
            }
            val botPad = when {
                centered -> 0.dp
                anchor != null && !fromTop -> with(d) { (constraints.maxHeight - anchor.top).toDp() } + 6.dp
                anchor != null && fromTop -> edgePad
                else -> fallbackPad
            }
            Box(
                modifier = modifier
                    .align(align)
                    .then(if (centered) Modifier else Modifier.padding(start = if (isLandscape) 14.dp else 20.dp, end = if (isLandscape) 14.dp else 20.dp, top = topPad, bottom = botPad))
                    .heightIn(max = if (centered) maxHeight else (maxHeight - topPad - botPad).coerceAtLeast(140.dp))
                    .onGloballyPositioned { lc ->
                        if (anchor != null) {
                            val r = lc.boundsInWindow()
                            if (r.width > 0f && r.height > 0f) {
                                origin = TransformOrigin(
                                    ((anchor.center.x - r.left) / r.width).coerceIn(0f, 1f),
                                    ((anchor.center.y - r.top) / r.height).coerceIn(0f, 1f)
                                )
                            }
                        }
                    }
                    .graphicsLayer {
                        val s = 0.15f + 0.85f * p
                        scaleX = s
                        scaleY = s
                        alpha = (p * 1.8f).coerceIn(0f, 1f)
                        transformOrigin = origin ?: TransformOrigin(if (fromStart) 0.1f else 0.9f, if (fromTop) 0f else 1f)
                    }
                    .pointerInput(Unit) { detectTapGestures { } }   // swallow taps inside panel
            ) { content() }
        }
    }
}

/** True when the OS will really blur windows behind a dialog (API 31+, not disabled by battery saver/GPU). */
@Composable
fun rememberCrossWindowBlur(): Boolean {
    val ctx = LocalContext.current
    return remember {
        Build.VERSION.SDK_INT >= 31 &&
            ctx.getSystemService(WindowManager::class.java).isCrossWindowBlurEnabled
    }
}

/** Frosted-glass panel: blurred backdrop (window blur) + smoky tint + top-lit frost + specular rim. */
fun Modifier.glassPanel(radius: Dp = 28.dp): Modifier = composed {
    val isGlassEnabled = com.styropyr0.prismal.PrismalGlass.isGlassmorphismEnabled
    val shape = RoundedCornerShape(radius)
    if (!isGlassEnabled) {
        return@composed this
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(Color(0xF2121418))
            .border(1.dp, Color(0x28FFFFFF), shape)
    }
    val prismalBackdrop = LocalPrismalBackdrop.current
    if (prismalBackdrop != null) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        return@composed this.drawPrismalGlass(
            backdrop = prismalBackdrop,
            shape = { shape },
            effects = {
                applyPrismalGlassEffects(
                    density = density,
                    adaptiveLuminance = false,
                    luminance = 0.5f,
                    blurRadiusPx = with(density) { 24.dp.toPx() },
                    refractionHeightPx = with(density) { 16.dp.toPx() },
                    refractionAmountPx = with(density) { 24.dp.toPx() },
                    useVibrancy = true,
                    saturation = 1.35f
                )
            },
            specular = {
                PrismalSpecular(
                    width = 1.dp,
                    blurRadius = 0.5.dp,
                    alpha = 0.9f,
                    style = PrismalSpecularStyle.Default
                )
            },
            depthShadow = {
                PrismalDepthShadow(radius = 24.dp, alpha = 0.9f)
            },
            onDrawSurface = {
                drawRect(Color(0xB310141E))
            }
        )
    }
    val blur = rememberCrossWindowBlur()
    this
        .shadow(24.dp, shape, clip = false, ambientColor = Color(0x66000000), spotColor = Color(0x66000000))
        .clip(shape)
        .background(if (blur) Color(0x5C10141B) else Color(0xF2121418))
        .background(Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x0AFFFFFF), Color(0x18000000))))
        .border(
            1.2.dp,
            Brush.linearGradient(
                listOf(Color(0x99FFFFFF), Color(0x1AFFFFFF), Color(0x0DFFFFFF), Color(0x47FFFFFF)),
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            shape
        )
}


/** Label + value + glass slider (default 0..100). */
@Composable
fun GlassSliderRow(
    label: String,
    valueText: String,
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..100f
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color(0xCCFFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(valueText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0x40FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth().height(28.dp)
        )
    }
}

// ==========================================
// 8. STAGED STREAM LOADER (4 animated scenes + progress + step counter)
// ==========================================

private const val STAGE_MS = 2200L
private const val SLOW_AFTER_S = 12


@Composable
fun CineStreamLoader(
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White,
    subtitle: String? = null,
    stages: List<String> = listOf(
        "Connecting to server",
        "Loading your stream",
        "Preparing the player",
        "Starting playback"
    )
) {
    val n = stages.size.coerceAtLeast(1)

    var stage by remember(stages) { mutableStateOf(0) }
    var secs by remember { mutableStateOf(0) }
    LaunchedEffect(stages) { while (stage < stages.lastIndex) { delay(STAGE_MS); stage++ } }
    LaunchedEffect(Unit) { while (true) { delay(1000); secs++ } }

    val last = stage >= n - 1
    val progress = animateFloatAsState(
        targetValue = if (last) 0.98f else (stage + 1f) / n * 0.9f,
        animationSpec = tween(if (last) 10000 else 1600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val currentDetail = when (stage) {
        0 -> "Connecting to server"
        1 -> subtitle?.let { "$it · Loading stream segments" } ?: "Loading stream segments"
        2 -> "Synchronizing playback buffers"
        else -> "Buffering high quality video"
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Basic clean circular loading animation
            CircularProgressIndicator(
                modifier = Modifier.size(46.dp),
                color = accentColor,
                trackColor = Color.White.copy(alpha = 0.15f),
                strokeWidth = 3.5.dp
            )

            // Smooth Primary Text Transformation
            AnimatedContent(
                targetState = stages[stage.coerceAtMost(stages.lastIndex)],
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 3 }) togetherWith
                        (fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 3 })
                },
                label = "stageText"
            ) { text ->
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Smooth Detail Transformation Info
            AnimatedContent(
                targetState = currentDetail,
                transitionSpec = {
                    fadeIn(tween(240)) togetherWith fadeOut(tween(160))
                },
                label = "detailText"
            ) { detailText ->
                Text(
                    text = detailText,
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            // Minimalist hairline progress track
            Canvas(Modifier.width(160.dp).height(2.dp)) {
                val r = CornerRadius(size.height / 2f)
                drawRoundRect(Color.White.copy(alpha = 0.12f), cornerRadius = r)
                drawRoundRect(
                    Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.35f), accentColor)),
                    size = Size(size.width * progress.value, size.height),
                    cornerRadius = r
                )
            }

            // Step & Time info pill
            Row(
                modifier = Modifier.width(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STEP ${stage + 1}/$n",
                    color = Color(0x70FFFFFF),
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${secs}s",
                    color = Color(0x70FFFFFF),
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (secs >= SLOW_AFTER_S) {
                Text(
                    text = "Taking longer than usual — optimizing connection",
                    color = Color(0xB3FFD27A),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
