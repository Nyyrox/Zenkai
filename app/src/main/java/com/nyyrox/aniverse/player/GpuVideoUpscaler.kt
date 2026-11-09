package com.nyyrox.aniverse.player

import android.app.ActivityManager
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicInteger
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Universal OpenGL ES 2.0 Real-Time Video Upscaler & Sharpener for Android (minSdk 21+).
 *
 * Implements hardware bilinear upscaling followed by a single-pass edge-adaptive AMD RCAS
 * (Robust Contrast Adaptive Sharpening) + light contour darkening with zero FBO overhead.
 */
object GpuUpscalerConstants {
    const val TAG = "GpuVideoUpscaler"

    // Resize modes matching Media3 AspectRatioFrameLayout
    const val RESIZE_MODE_FIT = 0
    const val RESIZE_MODE_FILL = 3
    const val RESIZE_MODE_ZOOM = 4

    // RCAS Vertex Shader: computes output-pixel offsets in vertex stage respecting SurfaceTexture orientation/rotation
    const val RCAS_VERTEX_SHADER = """
        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        uniform mat4 uSTMatrix;
        uniform vec2 uPx;
        varying vec2 vUv;
        varying vec2 vDx;
        varying vec2 vDy;

        void main() {
            gl_Position = aPosition;
            vUv = (uSTMatrix * aTexCoord).xy;
            vDx = (uSTMatrix * vec4(uPx.x, 0.0, 0.0, 0.0)).xy;
            vDy = (uSTMatrix * vec4(0.0, uPx.y, 0.0, 0.0)).xy;
        }
    """

    // 5-Tap RCAS + Contour Fragment Shader with anti-ringing clamp and dynamic precision
    const val RCAS_FRAGMENT_SHADER = """
        #extension GL_OES_EGL_image_external : require
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif

        uniform samplerExternalOES uTex;
        uniform float uSharp;    // 0.6..1.0
        uniform float uLine;     // 0.0..0.5
        uniform float uGain;
        varying vec2 vUv;
        varying vec2 vDx;
        varying vec2 vDy;

        float lum(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

        void main() {
            vec3 e = texture2D(uTex, vUv).rgb;
            vec3 b = texture2D(uTex, vUv - vDy).rgb;
            vec3 d = texture2D(uTex, vUv - vDx).rgb;
            vec3 f = texture2D(uTex, vUv + vDx).rgb;
            vec3 h = texture2D(uTex, vUv + vDy).rgb;

            // RCAS: scalar negative-lobe weight across channels to eliminate RGB chromatic dots
            vec3 mn = min(min(b, d), min(f, h));
            vec3 mx = max(max(b, d), max(f, h));
            vec3 hitMin = mn / (4.0 * mx + 0.0001);
            vec3 hitMax = (1.0 - mx) / (4.0 - 4.0 * mn + 0.0001);
            vec3 lobe = -min(hitMin, hitMax);
            float minW = min(lobe.r, min(lobe.g, lobe.b));
            float w = clamp(minW * uSharp, -0.1875, 0.0);
            vec3 o = (w * (b + d + f + h) + e) / (4.0 * w + 1.0);

            // Anti-ringing clamp to local neighborhood bounds
            o = clamp(o, min(mn, e), max(mx, e));

            // Cheap contour from the same cross taps (central differences)
            float gx = lum(f) - lum(d);
            float gy = lum(h) - lum(b);
            float edge = clamp(length(vec2(gx, gy)) * 2.0 * uGain, 0.0, 1.0);

            gl_FragColor = vec4(clamp(o * (1.0 - uLine * edge), 0.0, 1.0), 1.0);
        }
    """

    // Bypass Vertex Shader: 1-fetch passthrough
    const val BYPASS_VERTEX_SHADER = """
        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        uniform mat4 uSTMatrix;
        varying vec2 vUv;

        void main() {
            gl_Position = aPosition;
            vUv = (uSTMatrix * aTexCoord).xy;
        }
    """

    // 1-Tap Bypass Fragment Shader: zero math overhead when upscaling is inactive
    const val BYPASS_FRAGMENT_SHADER = """
        #extension GL_OES_EGL_image_external : require
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif

        uniform samplerExternalOES uTex;
        varying vec2 vUv;

        void main() {
            gl_FragColor = texture2D(uTex, vUv);
        }
    """
}

/**
 * Device tier detection logic for low-end device throttling.
 */
object GpuDeviceTier {
    fun isLowEnd(context: Context): Boolean {
        // ponytail: API < 26 indicates legacy GPU drivers with severe samplerExternalOES overhead.
        if (Build.VERSION.SDK_INT < 26) return true

        // ponytail: totalMem < 2.5GB (covers 3GB advertised devices which report ~2.8GB) reliably identifies bandwidth-constrained devices.
        return try {
            val actMgr = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actMgr?.getMemoryInfo(memInfo)
            memInfo.totalMem < (2.5 * 1024 * 1024 * 1024).toLong()
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * OpenGL ES 2.0 Renderer handling frame acquisition, shader execution, and resource lifecycle.
 */
class GlUpscalerRenderer(
    private val context: Context,
    private val onSurfaceReady: (Surface) -> Unit,
    private val onRequestRender: () -> Unit,
    private val getDroppedFrames: () -> Int
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private val isLowEndTier = GpuDeviceTier.isLowEnd(context)

    // GL Handles & Programs
    private var oesTextureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var outputSurface: Surface? = null

    private var rcasProgramId: Int = 0
    private var bypassProgramId: Int = 0
    private var isRcasCompiled: Boolean = false
    private var isBypassCompiled: Boolean = false

    // RCAS Locations
    private var rcasPosLoc: Int = -1
    private var rcasTexLoc: Int = -1
    private var rcasMatrixLoc: Int = -1
    private var rcasSamplerLoc: Int = -1
    private var rcasPxLoc: Int = -1
    private var rcasSharpLoc: Int = -1
    private var rcasLineLoc: Int = -1
    private var rcasGainLoc: Int = -1

    // Bypass Locations
    private var bypassPosLoc: Int = -1
    private var bypassTexLoc: Int = -1
    private var bypassMatrixLoc: Int = -1
    private var bypassSamplerLoc: Int = -1

    // ponytail: direct-allocated vertex and UV buffers reused across all frames; zero GC allocations during playback.
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(floatArrayOf(-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f))
            position(0)
        }
    }

    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
            position(0)
        }
    }

    private val stMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }

    // State & Dynamic Configuration
    @Volatile var isUpscalerEnabled: Boolean = true
    @Volatile var sharpness: Float = 0.8f
        set(value) {
            // ponytail: uniform clamping prevents extreme user parameters from causing halo ringing artifacts.
            field = value.coerceIn(0.6f, 1.0f)
        }

    @Volatile var lineDarkening: Float = 0.25f
        set(value) {
            // ponytail: line darkening is forced to 0.0 on low-end tier to eliminate edge gradient math on weak Mali/Adreno pipelines.
            field = if (isLowEndTier) 0.0f else value.coerceIn(0.0f, 0.5f)
        }

    @Volatile var resizeMode: Int = GpuUpscalerConstants.RESIZE_MODE_FIT
    @Volatile var sourceWidth: Int = 0
    @Volatile var sourceHeight: Int = 0
    @Volatile var outputWidth: Int = 0
    @Volatile var outputHeight: Int = 0
    @Volatile var currentScaleX: Float = 1.0f
    @Volatile var currentScaleY: Float = 1.0f

    @Volatile private var isFrameAvailable: Boolean = false

    // Frame drop evaluation on draw path
    private var lastWindowCheckMs: Long = SystemClock.uptimeMillis()
    private var renderedInWindow: Int = 0
    private var bypassUntilMs: Long = 0L
    private var trips: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val oldSurface = outputSurface
        val oldSurfaceTexture = surfaceTexture

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]

        // ponytail: hardware bilinear upscale is free on samplerExternalOES; no separate upscale pass or FBO chain required.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val st = SurfaceTexture(oesTextureId).apply {
            setOnFrameAvailableListener(this@GlUpscalerRenderer)
        }
        surfaceTexture = st
        val sf = Surface(st)
        outputSurface = sf

        // ponytail: surface swap creates and binds the new Surface to the decoder before releasing the old one, preventing codec underflow and black flashes.
        onSurfaceReady(sf)
        oldSurface?.release()
        oldSurfaceTexture?.release()

        // Compile Shader Programs
        initPrograms()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        outputWidth = width
        outputHeight = height
        GLES20.glViewport(0, 0, width, height)
        updateQuadGeometry()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            isFrameAvailable = true
        }
        // Wake up render loop only when a new decoded frame actually arrives
        onRequestRender()
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(this) {
            if (isFrameAvailable) {
                try {
                    surfaceTexture?.updateTexImage()
                    surfaceTexture?.getTransformMatrix(stMatrix)
                } catch (e: Exception) {
                    Log.w(GpuUpscalerConstants.TAG, "Frame update dropped", e)
                }
                isFrameAvailable = false
            }
        }

        // ponytail: letterbox clear color provides clean black bars without external container clipping.
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Evaluate frame drops every 2s on the draw path
        renderedInWindow++
        val now = SystemClock.uptimeMillis()
        if (now - lastWindowCheckMs >= 2000L) {
            val drops = getDroppedFrames()
            val total = drops + renderedInWindow
            if (total > 0) {
                val dropRatio = drops.toFloat() / total.toFloat()
                if (dropRatio > 0.05f) {
                    // ponytail: exponential backoff on frame-drop bypass (up to 16x 30s = 8min) prevents thermal flapping on sustained heavy loads.
                    bypassUntilMs = now + 30_000L * (1 shl minOf(trips++, 4))
                    Log.i(
                        GpuUpscalerConstants.TAG,
                        "Frame drops > 5% (${(dropRatio * 100).toInt()}%). Activating bypass with backoff trip $trips."
                    )
                }
            }
            renderedInWindow = 0
            lastWindowCheckMs = now
        }

        val isBypassActive = (now < bypassUntilMs)
        val activeHeight = outputHeight.toFloat() * currentScaleY
        // ponytail: videos within 10% of display height bypass RCAS because sharpening difference is visually imperceptible but saves GPU power.
        val isUpscaleBeneficial = sourceHeight > 0 &&
                activeHeight > 0 &&
                sourceHeight.toFloat() < (activeHeight * 0.90f)

        val isUpscaleNeeded = isUpscalerEnabled &&
                isUpscaleBeneficial &&
                !isBypassActive &&
                isRcasCompiled

        if (isUpscaleNeeded) {
            renderRcasPass()
        } else {
            renderBypassPass()
        }
    }

    private fun renderRcasPass() {
        GLES20.glUseProgram(rcasProgramId)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(rcasSamplerLoc, 0)

        GLES20.glEnableVertexAttribArray(rcasPosLoc)
        GLES20.glVertexAttribPointer(rcasPosLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(rcasTexLoc)
        GLES20.glVertexAttribPointer(rcasTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glUniformMatrix4fv(rcasMatrixLoc, 1, false, stMatrix, 0)

        // ponytail: uPx accounts for letterbox scale so offsets equal exactly one output screen pixel inside the video rectangle, preventing halo artifacts.
        val quadW = maxOf(1f, outputWidth.toFloat() * currentScaleX)
        val quadH = maxOf(1f, outputHeight.toFloat() * currentScaleY)
        GLES20.glUniform2f(rcasPxLoc, 1.0f / quadW, 1.0f / quadH)

        GLES20.glUniform1f(rcasSharpLoc, sharpness)
        // ponytail: line darkening is forced to 0.0 on low-end tier to eliminate edge gradient math on weak Mali/Adreno pipelines.
        GLES20.glUniform1f(rcasLineLoc, if (isLowEndTier) 0.0f else lineDarkening)

        // ponytail: edge gradient across bilinear upscale scales inversely with zoom; uGain restores edge prominence at 2-3x upscale.
        val scaleFactor = if (sourceHeight > 0) quadH / sourceHeight.toFloat() else 1.0f
        GLES20.glUniform1f(rcasGainLoc, scaleFactor.coerceIn(1f, 3f))

        // Draw Fullscreen Quad (ONE Fragment Pass, Zero Multi-Pass)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(rcasPosLoc)
        GLES20.glDisableVertexAttribArray(rcasTexLoc)
    }

    private fun renderBypassPass() {
        if (!isBypassCompiled) return

        GLES20.glUseProgram(bypassProgramId)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(bypassSamplerLoc, 0)

        GLES20.glEnableVertexAttribArray(bypassPosLoc)
        GLES20.glVertexAttribPointer(bypassPosLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(bypassTexLoc)
        GLES20.glVertexAttribPointer(bypassTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glUniformMatrix4fv(bypassMatrixLoc, 1, false, stMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(bypassPosLoc)
        GLES20.glDisableVertexAttribArray(bypassTexLoc)
    }

    fun updateQuadGeometry() {
        if (sourceWidth <= 0 || sourceHeight <= 0 || outputWidth <= 0 || outputHeight <= 0) {
            setQuadScale(1.0f, 1.0f)
            return
        }

        val videoAspect = sourceWidth.toFloat() / sourceHeight.toFloat()
        val viewAspect = outputWidth.toFloat() / outputHeight.toFloat()

        when (resizeMode) {
            GpuUpscalerConstants.RESIZE_MODE_FIT -> {
                if (videoAspect > viewAspect) {
                    setQuadScale(1.0f, viewAspect / videoAspect)
                } else {
                    setQuadScale(videoAspect / viewAspect, 1.0f)
                }
            }
            GpuUpscalerConstants.RESIZE_MODE_ZOOM -> {
                if (videoAspect > viewAspect) {
                    setQuadScale(videoAspect / viewAspect, 1.0f)
                } else {
                    setQuadScale(1.0f, viewAspect / videoAspect)
                }
            }
            else -> {
                setQuadScale(1.0f, 1.0f)
            }
        }
    }

    private fun setQuadScale(scaleX: Float, scaleY: Float) {
        currentScaleX = scaleX
        currentScaleY = scaleY
        vertexBuffer.position(0)
        vertexBuffer.put(floatArrayOf(
            -scaleX, -scaleY,
             scaleX, -scaleY,
            -scaleX,  scaleY,
             scaleX,  scaleY
        ))
        vertexBuffer.position(0)
    }

    private fun initPrograms() {
        if (rcasProgramId != 0) {
            GLES20.glDeleteProgram(rcasProgramId)
            rcasProgramId = 0
        }
        if (bypassProgramId != 0) {
            GLES20.glDeleteProgram(bypassProgramId)
            bypassProgramId = 0
        }

        // Compile Bypass Program First (Guaranteed Safe Baseline)
        val bypassProg = buildProgram(GpuUpscalerConstants.BYPASS_VERTEX_SHADER, GpuUpscalerConstants.BYPASS_FRAGMENT_SHADER)
        if (bypassProg != 0) {
            bypassProgramId = bypassProg
            bypassPosLoc = GLES20.glGetAttribLocation(bypassProg, "aPosition")
            bypassTexLoc = GLES20.glGetAttribLocation(bypassProg, "aTexCoord")
            bypassMatrixLoc = GLES20.glGetUniformLocation(bypassProg, "uSTMatrix")
            bypassSamplerLoc = GLES20.glGetUniformLocation(bypassProg, "uTex")
            isBypassCompiled = true
        }

        // Compile RCAS Program
        // ponytail: GL compile/link failure gracefully degrades to 1-fetch bypass shader without crashing or interrupting playback.
        val rcasProg = buildProgram(GpuUpscalerConstants.RCAS_VERTEX_SHADER, GpuUpscalerConstants.RCAS_FRAGMENT_SHADER)
        if (rcasProg != 0) {
            rcasProgramId = rcasProg
            rcasPosLoc = GLES20.glGetAttribLocation(rcasProg, "aPosition")
            rcasTexLoc = GLES20.glGetAttribLocation(rcasProg, "aTexCoord")
            rcasMatrixLoc = GLES20.glGetUniformLocation(rcasProg, "uSTMatrix")
            rcasSamplerLoc = GLES20.glGetUniformLocation(rcasProg, "uTex")
            rcasPxLoc = GLES20.glGetUniformLocation(rcasProg, "uPx")
            rcasSharpLoc = GLES20.glGetUniformLocation(rcasProg, "uSharp")
            rcasLineLoc = GLES20.glGetUniformLocation(rcasProg, "uLine")
            rcasGainLoc = GLES20.glGetUniformLocation(rcasProg, "uGain")
            isRcasCompiled = true
        } else {
            Log.e(GpuUpscalerConstants.TAG, "RCAS shader failed to link; falling back to bypass")
            isRcasCompiled = false
        }
    }

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vert = loadShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        if (vert == 0) return 0
        val frag = loadShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        if (frag == 0) {
            GLES20.glDeleteShader(vert)
            return 0
        }

        val prog = GLES20.glCreateProgram()
        if (prog == 0) return 0

        GLES20.glAttachShader(prog, vert)
        GLES20.glAttachShader(prog, frag)
        GLES20.glLinkProgram(prog)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        GLES20.glDeleteShader(vert)
        GLES20.glDeleteShader(frag)

        return if (linkStatus[0] != 0) prog else {
            GLES20.glDeleteProgram(prog)
            0
        }
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        return if (compiled[0] != 0) shader else {
            GLES20.glDeleteShader(shader)
            0
        }
    }

    fun releaseSurfaceObjects() {
        synchronized(this) {
            try {
                outputSurface?.release()
                outputSurface = null
                surfaceTexture?.release()
                surfaceTexture = null
            } catch (e: Exception) {
                Log.w(GpuUpscalerConstants.TAG, "Error releasing surface objects", e)
            }
        }
    }
}

/**
 * Drop-in GLSurfaceView for ExoPlayer that applies real-time hardware bilinear + RCAS upscaling.
 */
@OptIn(UnstableApi::class)
class GpuUpscalerSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs), AnalyticsListener, Player.Listener {

    private var playerRef: ExoPlayer? = null
    private var attachedSurface: Surface? = null
    private val isLowEndTier = GpuDeviceTier.isLowEnd(context)
    private val droppedFramesCount = AtomicInteger(0)

    // ponytail: Android requires secure SurfaceView for protected DRM content; SurfaceTexture cannot access protected memory and falls back to system surface.
    var isSecureContent: Boolean = false
        set(value) {
            field = value
            if (value) {
                detachPlayer()
                onSecureFallbackRequested?.invoke()
            }
        }

    var onSecureFallbackRequested: (() -> Unit)? = null

    val renderer = GlUpscalerRenderer(
        context = context,
        onSurfaceReady = { surface ->
            attachedSurface = surface
            post {
                if (!isSecureContent) {
                    playerRef?.setVideoSurface(surface)
                }
            }
        },
        onRequestRender = {
            requestRender()
        },
        getDroppedFrames = {
            droppedFramesCount.getAndSet(0)
        }
    )

    var resizeMode: Int
        get() = renderer.resizeMode
        set(value) {
            renderer.resizeMode = value
            queueEvent {
                renderer.updateQuadGeometry()
                requestRender()
            }
        }

    init {
        // Enforce OpenGL ES 2.0 (Works on 100% of Android devices minSdk 21+)
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (!isLowEndTier || w <= 0 || h <= 0) return
        val s = minOf(1920f / maxOf(w, h), 1080f / minOf(w, h))
        if (s < 1f) holder.setFixedSize((w * s).toInt(), (h * s).toInt())
    }

    /**
     * Connects an ExoPlayer instance to this GPU upscaled surface.
     */
    fun attachPlayer(player: ExoPlayer?) {
        if (playerRef === player) return
        detachPlayer()
        playerRef = player
        if (isSecureContent) {
            onSecureFallbackRequested?.invoke()
            return
        }
        player?.let { p ->
            p.addAnalyticsListener(this)
            p.addListener(this)
            attachedSurface?.let { s -> p.setVideoSurface(s) }
            val videoSize = p.videoSize
            val par = if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio else 1.0f
            renderer.sourceWidth = (videoSize.width * par).toInt()
            renderer.sourceHeight = videoSize.height
            queueEvent {
                renderer.updateQuadGeometry()
                requestRender()
            }
        }
    }

    /**
     * Detaches player and clears listeners.
     */
    fun detachPlayer() {
        playerRef?.let { p ->
            p.removeAnalyticsListener(this)
            p.removeListener(this)
            p.clearVideoSurface()
        }
        playerRef = null
    }

    /**
     * Dynamic configuration updates.
     */
    fun setUpscalerConfig(enabled: Boolean, sharp: Float = 0.8f, line: Float = 0.25f) {
        renderer.isUpscalerEnabled = enabled
        renderer.sharpness = sharp
        renderer.lineDarkening = line
        requestRender()
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        val par = if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio else 1.0f
        renderer.sourceWidth = (videoSize.width * par).toInt()
        renderer.sourceHeight = videoSize.height
        queueEvent {
            renderer.updateQuadGeometry()
            requestRender()
        }
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long
    ) {
        droppedFramesCount.addAndGet(droppedFrames)
    }

    override fun onDetachedFromWindow() {
        detachPlayer()
        renderer.releaseSurfaceObjects()
        super.onDetachedFromWindow() // ponytail: thread exit destroys the EGL context, freeing texture and programs
    }
}

/**
 * Jetpack Compose wrapper for GpuUpscalerSurfaceView.
 */
@Composable
fun GpuUpscaledPlayer(
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sharpness: Float = 0.8f,
    lineDarkening: Float = 0.25f,
    resizeMode: Int = GpuUpscalerConstants.RESIZE_MODE_FIT,
    isSecureContent: Boolean = false,
    secureFallbackContent: @Composable () -> Unit
) {
    if (isSecureContent) {
        // ponytail: Android requires secure SurfaceView for protected DRM content; SurfaceTexture cannot access protected memory and falls back to system surface.
        secureFallbackContent()
    } else {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                GpuUpscalerSurfaceView(ctx).apply {
                    this.isSecureContent = isSecureContent
                    attachPlayer(player)
                    setUpscalerConfig(enabled, sharpness, lineDarkening)
                    this.resizeMode = resizeMode
                }
            },
            update = { view ->
                view.isSecureContent = isSecureContent
                view.attachPlayer(player)
                view.setUpscalerConfig(enabled, sharpness, lineDarkening)
                view.resizeMode = resizeMode
            }
        )
    }
}
