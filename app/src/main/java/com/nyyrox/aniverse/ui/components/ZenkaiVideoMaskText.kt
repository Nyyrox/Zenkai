package com.nyyrox.aniverse.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily

@Composable
fun ZenkaiVideoMaskText(
    modifier: Modifier = Modifier,
    text: String = "ZENKAI",
    fontSize: TextUnit = 56.sp,
    letterSpacingRatio: Float = 0.12f,
    strokeColor: Color = Color(0xC0FFFFFF),
    strokeWidthDp: Dp = 1.5.dp,
    @RawRes videoRes: Int = R.raw.zenkai_bg
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val strokeWidthPx = with(density) { strokeWidthDp.toPx() }
    val strokeColorInt = strokeColor.toArgb()

    val typeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.russo_one) ?: Typeface.DEFAULT_BOLD
        } catch (_: Throwable) {
            Typeface.DEFAULT_BOLD
        }
    }

    AndroidView(
        factory = { ctx ->
            object : FrameLayout(ctx) {
                private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                private val textPath = android.graphics.Path()

                init {
                    setLayerType(LAYER_TYPE_HARDWARE, null)
                    clipChildren = true
                    clipToPadding = true

                    // 1. TextureView for playing rotated 90deg video
                    val textureView = TextureView(ctx).apply {
                        rotation = 90f
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            var player: MediaPlayer? = null

                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                try {
                                    val uri = Uri.parse("android.resource://" + ctx.packageName + "/" + videoRes)
                                    player = MediaPlayer().apply {
                                        setDataSource(ctx, uri)
                                        setSurface(Surface(st))
                                        isLooping = true
                                        setVolume(0f, 0f)
                                        setOnErrorListener { _, _, _ -> true }
                                        prepareAsync()
                                        setOnPreparedListener { mp ->
                                            try { mp.start() } catch (_: Throwable) {}
                                        }
                                    }
                                } catch (_: Throwable) {}
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                try {
                                    player?.stop()
                                    player?.release()
                                } catch (_: Throwable) {}
                                player = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                    addView(textureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

                    // Auto-scale TextureView to max dimension so 90-degree rotation fills the landscape text area
                    addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                        val w = right - left
                        val h = bottom - top
                        if (w > 0 && h > 0) {
                            val maxDim = maxOf(w, h)
                            val lp = textureView.layoutParams as? LayoutParams
                            if (lp != null && (lp.width != maxDim || lp.height != maxDim)) {
                                lp.width = maxDim
                                lp.height = maxDim
                                lp.gravity = android.view.Gravity.CENTER
                                textureView.layoutParams = lp
                            }
                        }
                    }
                }

                override fun dispatchDraw(canvas: Canvas) {
                    val w = width.toFloat()
                    val h = height.toFloat()
                    if (w <= 0f || h <= 0f) {
                        super.dispatchDraw(canvas)
                        return
                    }

                    textPaint.typeface = typeface
                    textPaint.textSize = fontSizePx
                    textPaint.letterSpacing = letterSpacingRatio
                    textPaint.textAlign = Paint.Align.CENTER

                    val fm = textPaint.fontMetrics
                    val baseline = (h / 2f) - ((fm.descent + fm.ascent) / 2f)

                    textPath.reset()
                    textPaint.getTextPath(text, 0, text.length, w / 2f, baseline, textPath)

                    // Clip the child TextureView video strictly inside the text glyphs
                    // No background is drawn anywhere on the canvas: 100% transparent behind & around the text!
                    canvas.save()
                    canvas.clipPath(textPath)
                    super.dispatchDraw(canvas)
                    canvas.restore()

                    // Draw crisp stroke around the letters for punchy contrast
                    if (strokeWidthPx > 0f && android.graphics.Color.alpha(strokeColorInt) > 0) {
                        strokePaint.typeface = typeface
                        strokePaint.textSize = fontSizePx
                        strokePaint.letterSpacing = letterSpacingRatio
                        strokePaint.textAlign = Paint.Align.CENTER
                        strokePaint.style = Paint.Style.STROKE
                        strokePaint.strokeWidth = strokeWidthPx
                        strokePaint.color = strokeColorInt
                        canvas.drawText(text, w / 2f, baseline, strokePaint)
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun ZenkaiCommunityRow(
    modifier: Modifier = Modifier,
    telegramUrl: String = "https://t.me/StreamverseOfc",
    discordUrl: String = "https://discord.gg/aXTfS6XGg"
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZenkaiSocialBadge(
            title = "JOIN DISCORD",
            iconColor = Color(0xFF5865F2),
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(discordUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        )

        Spacer(Modifier.width(10.dp))

        ZenkaiSocialBadge(
            title = "TELEGRAM",
            iconColor = Color(0xFF229ED9),
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        )
    }
}

@Composable
fun ZenkaiSocialBadge(
    title: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(iconColor)
            )
            Text(
                text = title,
                fontFamily = ZenkaiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.8.sp,
                color = Color.White
            )
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = Color(0x90FFFFFF),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}
