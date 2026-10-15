package com.nyyrox.aniverse.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

object GifCropHelper {

    fun isGif(bytes: ByteArray): Boolean {
        if (bytes.size < 6) return false
        val header = String(bytes, 0, 6)
        return header.startsWith("GIF87a") || header.startsWith("GIF89a")
    }

    fun isGif(context: Context, uri: Uri): Boolean {
        return try {
            val mime = context.contentResolver.getType(uri)
            if (mime?.contains("gif", ignoreCase = true) == true) return true
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buf = ByteArray(6)
                val read = stream.read(buf)
                if (read >= 6) {
                    val header = String(buf, 0, 6)
                    header.startsWith("GIF87a") || header.startsWith("GIF89a")
                } else false
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Crops a static image to a square region determined by normalized crop coordinates and rotation angle.
     * normalizedLeft, normalizedTop, normalizedSize are in range [0..1] relative to source dimensions.
     */
    suspend fun cropStaticImage(
        context: Context,
        uri: Uri,
        normLeft: Float,
        normTop: Float,
        normSize: Float,
        targetResolution: Int = 512,
        rotationAngle: Float = 0f
    ): ByteArray = withContext(Dispatchers.IO) {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not read image from URI")

        // 1. Decode bitmap with orientation fix
        var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: throw IllegalArgumentException("Failed to decode image bitmap")

        // Check EXIF orientation and apply user rotation
        try {
            val exif = ExifInterface(ByteArrayInputStream(rawBytes))
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            if (rotationAngle != 0f) {
                matrix.postRotate(rotationAngle)
            }
            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }
        } catch (_: Exception) {
            if (rotationAngle != 0f) {
                val matrix = Matrix().apply { postRotate(rotationAngle) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }
        }

        val srcW = bitmap.width
        val srcH = bitmap.height

        val cropW = (srcW * normSize).toInt().coerceIn(1, srcW)
        val cropH = (srcH * normSize).toInt().coerceIn(1, srcH)
        val cropSize = min(cropW, cropH)

        val cropX = (srcW * normLeft).toInt().coerceIn(0, srcW - cropSize)
        val cropY = (srcH * normTop).toInt().coerceIn(0, srcH - cropSize)

        val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropSize, cropSize)
        val scaled = if (cropSize > targetResolution) {
            Bitmap.createScaledBitmap(cropped, targetResolution, targetResolution, true)
        } else {
            cropped
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 95, out)
        if (scaled != cropped && scaled != bitmap) scaled.recycle()
        if (cropped != bitmap) cropped.recycle()
        bitmap.recycle()

        out.toByteArray()
    }

    /**
     * Crops an animated GIF frame by frame, preserving animation, frame delays, and rotation.
     */
    suspend fun cropAnimatedGif(
        context: Context,
        uri: Uri,
        normLeft: Float,
        normTop: Float,
        normSize: Float,
        targetResolution: Int = 320,
        rotationAngle: Float = 0f
    ): ByteArray = withContext(Dispatchers.Default) {
        val rawBytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: throw IllegalArgumentException("Could not read GIF from URI")

        val decoder = SimpleGifDecoder()
        val readStatus = decoder.read(ByteArrayInputStream(rawBytes))
        if (readStatus != SimpleGifDecoder.STATUS_OK || decoder.frameCount <= 0) {
            // Fallback to static crop if GIF decoding fails
            return@withContext cropStaticImage(context, uri, normLeft, normTop, normSize, targetResolution, rotationAngle)
        }

        val frameCount = decoder.frameCount
        val encoder = SimpleGifEncoder()
        val outStream = ByteArrayOutputStream()
        encoder.start(outStream)
        encoder.setRepeat(0) // Infinite loop

        val isSwapped = (rotationAngle.toInt() % 180 != 0)
        val srcW = if (isSwapped) decoder.height else decoder.width
        val srcH = if (isSwapped) decoder.width else decoder.height

        val cropSize = (min(srcW, srcH) * normSize).toInt().coerceIn(1, min(srcW, srcH))
        val cropX = (srcW * normLeft).toInt().coerceIn(0, max(0, srcW - cropSize))
        val cropY = (srcH * normTop).toInt().coerceIn(0, max(0, srcH - cropSize))

        for (i in 0 until frameCount) {
            var frameBmp = decoder.getFrame(i) ?: continue
            val delay = decoder.getDelay(i).coerceAtLeast(20) // ms
            encoder.setDelay(delay)

            if (rotationAngle != 0f) {
                val rotMatrix = Matrix().apply { postRotate(rotationAngle) }
                val rotated = Bitmap.createBitmap(frameBmp, 0, 0, frameBmp.width, frameBmp.height, rotMatrix, true)
                if (rotated != frameBmp) {
                    frameBmp.recycle()
                    frameBmp = rotated
                }
            }

            val safeCropSize = min(cropSize, min(frameBmp.width - cropX.coerceAtMost(frameBmp.width - 1), frameBmp.height - cropY.coerceAtMost(frameBmp.height - 1))).coerceAtLeast(1)
            val cropped = Bitmap.createBitmap(frameBmp, cropX.coerceIn(0, frameBmp.width - safeCropSize), cropY.coerceIn(0, frameBmp.height - safeCropSize), safeCropSize, safeCropSize)
            val finalFrame = if (safeCropSize > targetResolution) {
                Bitmap.createScaledBitmap(cropped, targetResolution, targetResolution, true)
            } else {
                cropped
            }

            encoder.addFrame(finalFrame)

            if (finalFrame != cropped && finalFrame != frameBmp) finalFrame.recycle()
            if (cropped != frameBmp) cropped.recycle()
            frameBmp.recycle()
        }

        encoder.finish()
        outStream.toByteArray()
    }
}

/**
 * Lightweight GIF decoder in pure Kotlin.
 */
class SimpleGifDecoder {
    companion object {
        const val STATUS_OK = 0
        const val STATUS_FORMAT_ERROR = 1
        const val STATUS_OPEN_ERROR = 2
    }

    var width: Int = 0
    var height: Int = 0
    var frameCount: Int = 0
    private val frames = mutableListOf<GifFrame>()

    data class GifFrame(val image: Bitmap, val delay: Int)

    fun getFrame(n: Int): Bitmap? = if (n in 0 until frames.size) frames[n].image else null
    fun getDelay(n: Int): Int = if (n in 0 until frames.size) frames[n].delay else 100

    fun read(`is`: InputStream): Int {
        frames.clear()
        frameCount = 0
        try {
            val bytes = `is`.readBytes()
            var pos = 0

            if (bytes.size < 13) return STATUS_FORMAT_ERROR
            val header = String(bytes, 0, 6)
            if (!header.startsWith("GIF")) return STATUS_FORMAT_ERROR
            pos = 6

            // Logical Screen Descriptor
            width = readShort(bytes, pos)
            height = readShort(bytes, pos + 2)
            val packed = bytes[pos + 4].toInt() and 0xFF
            val gctFlag = (packed and 0x80) != 0
            val gctSize = 2 shl (packed and 0x07)
            pos += 7

            var gct: IntArray? = null
            if (gctFlag) {
                gct = readColorTable(bytes, pos, gctSize)
                pos += gctSize * 3
            }

            var gceDelay = 100
            var gceTransIndex = -1
            var gceDisposal = 0

            var currentBitmap = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888)
            var canvas = Canvas(currentBitmap)

            while (pos < bytes.size) {
                val blockType = bytes[pos++].toInt() and 0xFF
                if (blockType == 0x3B) {
                    // Trailer
                    break
                } else if (blockType == 0x21) {
                    // Extension
                    if (pos >= bytes.size) break
                    val extCode = bytes[pos++].toInt() and 0xFF
                    if (extCode == 0xF9) {
                        // Graphic Control Extension
                        val blockSize = bytes[pos++].toInt() and 0xFF
                        if (blockSize >= 4 && pos + blockSize <= bytes.size) {
                            val extPacked = bytes[pos].toInt() and 0xFF
                            gceDisposal = (extPacked and 0x1C) shr 2
                            val transFlag = (extPacked and 0x01) != 0
                            gceDelay = readShort(bytes, pos + 1) * 10
                            if (gceDelay <= 0) gceDelay = 100
                            gceTransIndex = if (transFlag) bytes[pos + 3].toInt() and 0xFF else -1
                            pos += blockSize
                        }
                        // Skip sub-blocks until terminator
                        while (pos < bytes.size && bytes[pos].toInt() != 0) {
                            pos += (bytes[pos].toInt() and 0xFF) + 1
                        }
                        if (pos < bytes.size) pos++ // Skip 0x00
                    } else {
                        // Other extension: skip sub-blocks
                        while (pos < bytes.size && bytes[pos].toInt() != 0) {
                            pos += (bytes[pos].toInt() and 0xFF) + 1
                        }
                        if (pos < bytes.size) pos++
                    }
                } else if (blockType == 0x2C) {
                    // Image Descriptor
                    if (pos + 9 > bytes.size) break
                    val ix = readShort(bytes, pos)
                    val iy = readShort(bytes, pos + 2)
                    val iw = readShort(bytes, pos + 4)
                    val ih = readShort(bytes, pos + 6)
                    val ipacked = bytes[pos + 8].toInt() and 0xFF
                    val lctFlag = (ipacked and 0x80) != 0
                    val interlace = (ipacked and 0x40) != 0
                    val lctSize = 2 shl (ipacked and 0x07)
                    pos += 9

                    val act = if (lctFlag) {
                        val ct = readColorTable(bytes, pos, lctSize)
                        pos += lctSize * 3
                        ct
                    } else gct

                    if (pos >= bytes.size) break
                    val lzwMinCodeSize = bytes[pos++].toInt() and 0xFF

                    // Read image data sub-blocks
                    val lzwData = ByteArrayOutputStream()
                    while (pos < bytes.size) {
                        val subSize = bytes[pos++].toInt() and 0xFF
                        if (subSize == 0) break
                        if (pos + subSize <= bytes.size) {
                            lzwData.write(bytes, pos, subSize)
                            pos += subSize
                        } else break
                    }

                    if (iw > 0 && ih > 0 && act != null) {
                        val pixels = decodeLzw(lzwData.toByteArray(), iw * ih, lzwMinCodeSize)
                        val frameBmp = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888)
                        val colorPixels = IntArray(iw * ih)
                        for (idx in pixels.indices) {
                            val colorIdx = pixels[idx].toInt() and 0xFF
                            colorPixels[idx] = if (colorIdx == gceTransIndex) 0x00000000 else act.getOrElse(colorIdx) { 0xFF000000.toInt() }
                        }
                        frameBmp.setPixels(colorPixels, 0, iw, 0, 0, iw, ih)

                        val composedFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val compCanvas = Canvas(composedFrame)
                        compCanvas.drawBitmap(currentBitmap, 0f, 0f, null)
                        compCanvas.drawBitmap(frameBmp, ix.toFloat(), iy.toFloat(), null)

                        frames.add(GifFrame(composedFrame, gceDelay))
                        frameCount++

                        if (gceDisposal == 2) {
                            // Restore to background
                            currentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            canvas = Canvas(currentBitmap)
                        } else if (gceDisposal == 3) {
                            // Restore to previous (keep currentBitmap as is)
                        } else {
                            // Do not dispose / leave in place
                            canvas.drawBitmap(frameBmp, ix.toFloat(), iy.toFloat(), null)
                        }
                        frameBmp.recycle()
                    }
                }
            }
            return if (frameCount > 0) STATUS_OK else STATUS_FORMAT_ERROR
        } catch (e: Exception) {
            e.printStackTrace()
            return STATUS_OPEN_ERROR
        }
    }

    private fun readShort(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readColorTable(bytes: ByteArray, offset: Int, count: Int): IntArray {
        val colors = IntArray(count)
        var cur = offset
        for (i in 0 until count) {
            if (cur + 2 < bytes.size) {
                val r = bytes[cur++].toInt() and 0xFF
                val g = bytes[cur++].toInt() and 0xFF
                val b = bytes[cur++].toInt() and 0xFF
                colors[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return colors
    }

    private fun decodeLzw(data: ByteArray, pixelCount: Int, minCodeSize: Int): ByteArray {
        val pixels = ByteArray(pixelCount)
        val clearCode = 1 shl minCodeSize
        val eoiCode = clearCode + 1
        var available = clearCode + 2
        var oldCode = -1
        var codeSize = minCodeSize + 1
        var codeMask = (1 shl codeSize) - 1

        val prefix = IntArray(4096)
        val suffix = ByteArray(4096)
        val pixelStack = ByteArray(4097)

        for (i in 0 until clearCode) {
            prefix[i] = 0
            suffix[i] = i.toByte()
        }

        var datum = 0
        var bits = 0
        var top = 0
        var pi = 0
        var byteIndex = 0

        while (pi < pixelCount && byteIndex < data.size) {
            if (top == 0) {
                if (bits < codeSize) {
                    datum = datum or ((data[byteIndex++].toInt() and 0xFF) shl bits)
                    bits += 8
                    continue
                }
                val code = datum and codeMask
                datum = datum shr codeSize
                bits -= codeSize

                if (code > available || code == eoiCode) break
                if (code == clearCode) {
                    codeSize = minCodeSize + 1
                    codeMask = (1 shl codeSize) - 1
                    available = clearCode + 2
                    oldCode = -1
                    continue
                }
                if (oldCode == -1) {
                    pixelStack[top++] = suffix[code]
                    oldCode = code
                    continue
                }

                var inCode = code
                if (code == available) {
                    pixelStack[top++] = suffix[oldCode]
                    inCode = oldCode
                }
                while (inCode > clearCode) {
                    pixelStack[top++] = suffix[inCode]
                    inCode = prefix[inCode]
                }
                val first = suffix[inCode].toInt() and 0xFF
                pixelStack[top++] = first.toByte()

                if (available < 4096) {
                    prefix[available] = oldCode
                    suffix[available] = first.toByte()
                    available++
                    if (available and codeMask == 0 && available < 4096) {
                        codeSize++
                        codeMask += available
                    }
                }
                oldCode = code
            }
            top--
            pixels[pi++] = pixelStack[top]
        }
        return pixels
    }
}

/**
 * Lightweight Animated GIF Encoder in pure Kotlin.
 */
class SimpleGifEncoder {
    private var width = 0
    private var height = 0
    private var delay = 100 // ms
    private var repeat = 0  // 0 = infinite
    private var out: OutputStream? = null
    private var isFirstFrame = true

    fun start(os: OutputStream) {
        out = os
        writeString("GIF89a")
    }

    fun setDelay(ms: Int) {
        delay = ms
    }

    fun setRepeat(r: Int) {
        repeat = r
    }

    fun addFrame(bmp: Bitmap): Boolean {
        if (out == null) return false
        width = bmp.width
        height = bmp.height

        val pixels = IntArray(width * height)
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)

        // Generate 256-color palette (quantization)
        val colorMap = mutableMapOf<Int, Int>()
        val palette = mutableListOf<Int>()
        val indexedPixels = ByteArray(pixels.size)

        var transIndex = -1
        for (i in pixels.indices) {
            val color = pixels[i]
            val alpha = (color ushr 24) and 0xFF
            if (alpha < 128) {
                if (transIndex == -1) {
                    transIndex = palette.size
                    palette.add(0x000000)
                }
                indexedPixels[i] = transIndex.toByte()
            } else {
                // Approximate 6-6-6 or direct color
                val rgb = color and 0x00FFFFFF
                val idx = colorMap.getOrPut(rgb) {
                    if (palette.size < 256) {
                        val newIdx = palette.size
                        palette.add(rgb)
                        newIdx
                    } else {
                        // Nearest color match
                        var bestIdx = 0
                        var bestDist = Int.MAX_VALUE
                        val r = (rgb shr 16) and 0xFF
                        val g = (rgb shr 8) and 0xFF
                        val b = rgb and 0xFF
                        for (p in palette.indices) {
                            if (p == transIndex) continue
                            val pr = (palette[p] shr 16) and 0xFF
                            val pg = (palette[p] shr 8) and 0xFF
                            val pb = palette[p] and 0xFF
                            val dist = (r - pr) * (r - pr) + (g - pg) * (g - pg) + (b - pb) * (b - pb)
                            if (dist < bestDist) {
                                bestDist = dist
                                bestIdx = p
                            }
                        }
                        bestIdx
                    }
                }
                indexedPixels[i] = idx.toByte()
            }
        }

        // Pad palette to power of 2
        while (palette.size < 256) palette.add(0)

        if (isFirstFrame) {
            writeLSD()
            writePalette(palette)
            if (repeat >= 0) writeNetscapeExt()
            isFirstFrame = false
        }

        writeGraphicControlExt(transIndex)
        writeImageDescriptor()
        writePalette(palette)
        writePixels(indexedPixels)
        return true
    }

    fun finish(): Boolean {
        if (out == null) return false
        out?.write(0x3B) // GIF trailer
        out?.flush()
        return true
    }

    private fun writeLSD() {
        writeShort(width)
        writeShort(height)
        out?.write(0xF7) // Global Color Table Flag, 8 bits/pixel, 256 colors
        out?.write(0)    // Background Color Index
        out?.write(0)    // Pixel Aspect Ratio
    }

    private fun writeImageDescriptor() {
        out?.write(0x2C)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        out?.write(0x87) // Local Color Table Flag (8 bits/pixel, 256 colors)
    }

    private fun writeGraphicControlExt(transIndex: Int) {
        out?.write(0x21)
        out?.write(0xF9)
        out?.write(4) // Block size
        val transFlag = if (transIndex != -1) 1 else 0
        out?.write(transFlag or 0x08) // Disposal: restore to background, transparency flag
        writeShort((delay / 10).coerceAtLeast(1))
        out?.write(if (transIndex != -1) transIndex else 0)
        out?.write(0) // Terminator
    }

    private fun writeNetscapeExt() {
        out?.write(0x21)
        out?.write(0xFF)
        out?.write(11)
        writeString("NETSCAPE2.0")
        out?.write(3)
        out?.write(1)
        writeShort(repeat)
        out?.write(0)
    }

    private fun writePalette(palette: List<Int>) {
        for (color in palette) {
            out?.write((color shr 16) and 0xFF)
            out?.write((color shr 8) and 0xFF)
            out?.write(color and 0xFF)
        }
    }

    private fun writePixels(pixels: ByteArray) {
        val initCodeSize = 8
        out?.write(initCodeSize)

        // Simple LZW compression
        val clearCode = 1 shl initCodeSize
        val eoiCode = clearCode + 1

        val buffer = ByteArrayOutputStream()
        var curCodeSize = initCodeSize + 1
        var bitPos = 0
        var bitBuf = 0

        fun writeBits(code: Int, size: Int) {
            bitBuf = bitBuf or (code shl bitPos)
            bitPos += size
            while (bitPos >= 8) {
                buffer.write(bitBuf and 0xFF)
                bitBuf = bitBuf shr 8
                bitPos -= 8
            }
        }

        writeBits(clearCode, curCodeSize)

        val dict = HashMap<String, Int>()
        var nextCode = eoiCode + 1
        var prefix = ""

        for (b in pixels) {
            val c = (b.toInt() and 0xFF).toString()
            val combined = if (prefix.isEmpty()) c else "$prefix,$c"
            if (dict.containsKey(combined) || (prefix.isEmpty() && c.toInt() < clearCode)) {
                prefix = combined
            } else {
                val codeToWrite = if (prefix.contains(",")) dict[prefix] ?: 0 else prefix.toInt()
                writeBits(codeToWrite, curCodeSize)
                if (nextCode < 4096) {
                    dict[combined] = nextCode++
                    if (nextCode > (1 shl curCodeSize) && curCodeSize < 12) {
                        curCodeSize++
                    }
                } else {
                    writeBits(clearCode, curCodeSize)
                    dict.clear()
                    nextCode = eoiCode + 1
                    curCodeSize = initCodeSize + 1
                }
                prefix = c
            }
        }

        if (prefix.isNotEmpty()) {
            val codeToWrite = if (prefix.contains(",")) dict[prefix] ?: 0 else prefix.toInt()
            writeBits(codeToWrite, curCodeSize)
        }

        writeBits(eoiCode, curCodeSize)
        if (bitPos > 0) buffer.write(bitBuf and 0xFF)

        // Write sub-blocks
        val compressedBytes = buffer.toByteArray()
        var offset = 0
        while (offset < compressedBytes.size) {
            val chunkSize = min(255, compressedBytes.size - offset)
            out?.write(chunkSize)
            out?.write(compressedBytes, offset, chunkSize)
            offset += chunkSize
        }
        out?.write(0) // Terminator
    }

    private fun writeShort(value: Int) {
        out?.write(value and 0xFF)
        out?.write((value shr 8) and 0xFF)
    }

    private fun writeString(s: String) {
        out?.write(s.toByteArray(Charsets.US_ASCII))
    }
}
