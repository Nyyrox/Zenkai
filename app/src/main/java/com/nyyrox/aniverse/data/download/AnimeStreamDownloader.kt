package com.nyyrox.aniverse.data.download

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

data class HlsSegment(
    val index: Int,
    val uri: String,
    val durationSec: Double,
    val byteRangeOffset: Long? = null,
    val byteRangeLength: Long? = null,
    val keyUri: String? = null,
    val keyIv: ByteArray? = null,
    val sequenceNumber: Long = 0L,
    val isAudio: Boolean = false
)

data class HlsAudioTrackInfo(
    val index: Int,
    val name: String,
    val groupId: String,
    val language: String?,
    val isDefault: Boolean,
    val mediaInfo: HlsMediaInfo
)

data class HlsMediaInfo(
    val isMaster: Boolean,
    val targetUrl: String,
    val initSegmentUri: String? = null,
    val isFragmentedMp4: Boolean = false,
    val segments: List<HlsSegment> = emptyList(),
    val isEncrypted: Boolean = false,
    val totalDurationSec: Double = 0.0,
    val audioTracks: List<HlsAudioTrackInfo> = emptyList()
)

private data class VariantEntry(
    val uri: String,
    val bandwidth: Long,
    val resolutionHeight: Int,
    val name: String,
    val audioGroupId: String?
)

private data class AudioTrackEntry(
    val uri: String,
    val groupId: String,
    val name: String,
    val language: String?,
    val isDefault: Boolean
)

class AnimeStreamDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()
) {
    companion object {
        private const val TAG = "AnimeDownloader"
        private const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
        private const val TS_PACKET_SIZE = 188
        private const val TS_SYNC_BYTE: Byte = 0x47
    }

    /**
     * Downloads an HLS or direct MP4 stream into a consolidated, keyframe-synchronized video file.
     */
    suspend fun downloadStream(
        streamUrl: String,
        outputDir: File,
        headers: Map<String, String> = emptyMap(),
        preferredQuality: String? = null,
        preferredAudio: String? = null,
        isCancelled: () -> Boolean = { false },
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long, speedFormatted: String, completedSegments: Int, totalSegments: Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!outputDir.exists()) outputDir.mkdirs()

        val cleanUrl = streamUrl.trim()
        val isDirectMp4 = cleanUrl.endsWith(".mp4", ignoreCase = true) ||
                cleanUrl.contains(".mp4?", ignoreCase = true) ||
                cleanUrl.endsWith(".mkv", ignoreCase = true)

        if (isDirectMp4) {
            val destFile = File(outputDir, "video.mp4")
            downloadDirectFile(cleanUrl, destFile, headers, isCancelled, onProgress)
            return@withContext destFile
        }

        // 1. Resolve Master Playlist or Media Playlist with preferred Quality & Audio
        val mediaInfo = parseHlsPlaylist(cleanUrl, headers, preferredQuality, preferredAudio)
        if (mediaInfo.segments.isEmpty()) {
            // Fallback: If it's not a valid HLS manifest, attempt direct progressive download
            val destFile = File(outputDir, "video.mp4")
            downloadDirectFile(cleanUrl, destFile, headers, isCancelled, onProgress)
            return@withContext destFile
        }

        val videoSegments = mediaInfo.segments
        val audioTracks = mediaInfo.audioTracks
        val totalAudioSegments = audioTracks.sumOf { it.mediaInfo.segments.size }
        val totalSegments = videoSegments.size + totalAudioSegments

        val isFmp4 = mediaInfo.isFragmentedMp4 || mediaInfo.initSegmentUri != null
        val outputExt = if (isFmp4) "mp4" else "ts"
        val consolidatedFile = File(outputDir, "video.$outputExt")
        val tempCombinedFile = File(outputDir, "video.tmp")

        val segmentsDir = File(outputDir, "segments").apply { if (!exists()) mkdirs() }
        try {
            segmentsDir.listFiles { f -> f.name.endsWith(".part") }?.forEach { it.delete() }
        } catch (_: Exception) {}

        // 2. Fetch AES-128 Keys Cache for video & all audio tracks
        val keysCache = mutableMapOf<String, ByteArray>()
        val allSegments = mutableListOf<HlsSegment>().apply {
            addAll(videoSegments)
            audioTracks.forEach { addAll(it.mediaInfo.segments) }
        }
        for (seg in allSegments) {
            val kUri = seg.keyUri
            if (!kUri.isNullOrBlank() && !keysCache.containsKey(kUri)) {
                try {
                    val keyBytes = downloadBytes(kUri, headers)
                    if (keyBytes.size == 16) {
                        keysCache[kUri] = keyBytes
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch AES key from $kUri: ${e.message}")
                }
            }
        }

        // 3. Download Init Segment(s) (if Fragmented MP4 / EXT-X-MAP)
        var initSegmentBytes: ByteArray? = null
        val videoInitFile = File(segmentsDir, "video_init.mp4")
        if (!mediaInfo.initSegmentUri.isNullOrBlank()) {
            if (videoInitFile.exists() && videoInitFile.length() >= 8L) {
                initSegmentBytes = videoInitFile.readBytes()
            } else {
                try {
                    val bytes = downloadBytes(mediaInfo.initSegmentUri, headers)
                    initSegmentBytes = bytes
                    videoInitFile.writeBytes(bytes)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to download video init segment: ${e.message}")
                }
            }
        }

        // Save each audio track's init segment (fMP4 audio can't be decoded without it, needed for export/mux)
        audioTracks.forEach { track ->
            val initUri = track.mediaInfo.initSegmentUri
            val initFile = File(segmentsDir, "audio_${track.index}_init.mp4")
            if (!initUri.isNullOrBlank()) {
                if (!initFile.exists() || initFile.length() < 8L) {
                    try {
                        initFile.writeBytes(downloadBytes(initUri, headers))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to download audio init segment: ${e.message}")
                    }
                }
            } else if ((track.mediaInfo.isFragmentedMp4 || isFmp4) && !initFile.exists()) {
                // If audio playlist didn't specify distinct init segment, copy video init as fallback
                if (initSegmentBytes != null && initSegmentBytes.isNotEmpty()) {
                    initFile.writeBytes(initSegmentBytes)
                }
            }
        }

        // Count pre-existing segments and bytes to accurately calculate network download speed on resume
        val preExistingBytes = AtomicLong(0L)
        val preExistingCount = AtomicInteger(0)
        for (seg in videoSegments) {
            val f = File(segmentsDir, "video_seg_${String.format("%05d", seg.index)}.${if (isFmp4) "m4s" else "ts"}")
            if (f.exists() && f.length() >= 100L) {
                preExistingBytes.addAndGet(f.length())
                preExistingCount.incrementAndGet()
            }
        }
        audioTracks.forEach { track ->
            val trackIdx = track.index
            val isTrackFmp4 = track.mediaInfo.isFragmentedMp4 || track.mediaInfo.initSegmentUri != null
            val audioExt = if (isTrackFmp4) "m4s" else "aac"
            track.mediaInfo.segments.forEach { seg ->
                val f = File(segmentsDir, "audio_${trackIdx}_seg_${String.format("%05d", seg.index)}.$audioExt")
                if (f.exists() && f.length() >= 100L) {
                    preExistingBytes.addAndGet(f.length())
                    preExistingCount.incrementAndGet()
                }
            }
        }

        // 4. Concurrently download all video and audio segments with concurrency limiter (4 concurrent downloads)
        val concurrency = 4
        val semaphore = Semaphore(concurrency)
        val downloadedCount = AtomicInteger(0)
        val totalDownloadedBytes = AtomicLong(0L)
        val startTime = System.currentTimeMillis()
        var lastReportTime = 0L

        val videoSegmentFiles = Array<File?>(videoSegments.size) { null }

        coroutineScope {
            // Video download tasks
            val videoJobs = videoSegments.map { seg ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        if (isCancelled()) throw CancellationException("Download cancelled by user")
                        ensureActive()

                        val segFile = File(segmentsDir, "video_seg_${String.format("%05d", seg.index)}.${if (isFmp4) "m4s" else "ts"}")
                        if (!segFile.exists() || segFile.length() < 100L) {
                            val rawBytes = downloadSegmentBytesWithRetry(seg, headers, isCancelled)
                            val finalBytes = if (seg.keyUri != null && keysCache.containsKey(seg.keyUri)) {
                                decryptAes128(rawBytes, keysCache[seg.keyUri]!!, seg.keyIv, seg.sequenceNumber)
                            } else {
                                rawBytes
                            }
                            val cleanedBytes = if (!isFmp4) cleanTsStream(finalBytes) else finalBytes
                            val partFile = File(segmentsDir, "${segFile.name}.part")
                            partFile.writeBytes(cleanedBytes)
                            if (segFile.exists()) segFile.delete()
                            partFile.renameTo(segFile)
                        }

                        videoSegmentFiles[seg.index] = segFile
                        val currentCount = downloadedCount.incrementAndGet()
                        val currentBytes = totalDownloadedBytes.addAndGet(segFile.length())

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 100 || currentCount == totalSegments) {
                            lastReportTime = now
                            val elapsedSec = (now - startTime) / 1000.0
                            val networkBytes = currentBytes - preExistingBytes.get()
                            val speedBytesPerSec = if (elapsedSec > 0.5) (maxOf(0L, networkBytes) / elapsedSec).toLong() else 0L
                            val speedFormatted = formatSpeed(speedBytesPerSec)
                            val rawProg1 = if (totalSegments > 0) (currentCount.toFloat() / totalSegments).coerceIn(0f, 0.98f) else 0f
                            val progress = if (rawProg1.isNaN() || !rawProg1.isFinite()) 0f else rawProg1
                            onProgress(progress, currentBytes, currentBytes * totalSegments / maxOf(currentCount, 1), speedFormatted, currentCount, totalSegments)
                        }
                    }
                }
            }

            // Audio download tasks across all audio tracks
            val audioJobs = audioTracks.flatMap { track ->
                val trackIdx = track.index
                val isTrackFmp4 = track.mediaInfo.isFragmentedMp4 || track.mediaInfo.initSegmentUri != null
                val audioExt = if (isTrackFmp4) "m4s" else "aac"
                track.mediaInfo.segments.map { seg ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            if (isCancelled()) throw CancellationException("Download cancelled by user")
                            ensureActive()

                            val segFile = File(segmentsDir, "audio_${trackIdx}_seg_${String.format("%05d", seg.index)}.$audioExt")
                            if (!segFile.exists() || segFile.length() < 100L) {
                                val rawBytes = downloadSegmentBytesWithRetry(seg, headers, isCancelled)
                                val finalBytes = if (seg.keyUri != null && keysCache.containsKey(seg.keyUri)) {
                                    decryptAes128(rawBytes, keysCache[seg.keyUri]!!, seg.keyIv, seg.sequenceNumber)
                                } else {
                                    rawBytes
                                }
                                val partFile = File(segmentsDir, "${segFile.name}.part")
                                partFile.writeBytes(finalBytes)
                                if (segFile.exists()) segFile.delete()
                                partFile.renameTo(segFile)
                            }

                            val currentCount = downloadedCount.incrementAndGet()
                            val currentBytes = totalDownloadedBytes.addAndGet(segFile.length())

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 100 || currentCount == totalSegments) {
                                lastReportTime = now
                                val elapsedSec = (now - startTime) / 1000.0
                                val networkBytes = currentBytes - preExistingBytes.get()
                                val speedBytesPerSec = if (elapsedSec > 0.5) (maxOf(0L, networkBytes) / elapsedSec).toLong() else 0L
                                val speedFormatted = formatSpeed(speedBytesPerSec)
                                val rawProg1 = if (totalSegments > 0) (currentCount.toFloat() / totalSegments).coerceIn(0f, 0.98f) else 0f
                            val progress = if (rawProg1.isNaN() || !rawProg1.isFinite()) 0f else rawProg1
                                onProgress(progress, currentBytes, currentBytes * totalSegments / maxOf(currentCount, 1), speedFormatted, currentCount, totalSegments)
                            }
                        }
                    }
                }
            }

            videoJobs.awaitAll()
            audioJobs.awaitAll()
        }

        // 5. Combine and align video segments into consolidated video file
        onProgress(0.99f, totalDownloadedBytes.get(), totalDownloadedBytes.get(), "Finalizing...", totalSegments, totalSegments)

        BufferedOutputStream(FileOutputStream(tempCombinedFile)).use { outStream ->
            if (initSegmentBytes != null && initSegmentBytes.isNotEmpty()) {
                outStream.write(initSegmentBytes)
            }

            for (i in 0 until videoSegments.size) {
                val f = videoSegmentFiles[i] ?: File(segmentsDir, "video_seg_${String.format("%05d", i)}.${if (isFmp4) "m4s" else "ts"}")
                if (f.exists() && f.length() > 0L) {
                    val bytes = f.readBytes()
                    if (!isFmp4) {
                        val validBytes = cleanTsStream(bytes)
                        outStream.write(validBytes)
                    } else {
                        outStream.write(bytes)
                    }
                }
            }
            outStream.flush()
        }

        if (consolidatedFile.exists()) consolidatedFile.delete()
        tempCombinedFile.renameTo(consolidatedFile)

        // 6. Generate local offline M3U8 for seamless ExoPlayer offline HLS playback (with all audio tracks)
        generateOfflineM3u8(outputDir, mediaInfo, isFmp4)

        onProgress(1.0f, consolidatedFile.length(), consolidatedFile.length(), "Completed", totalSegments, totalSegments)
        consolidatedFile
    }

    /**
     * MPEG-TS Stream Alignment Cleaner
     * Strips leading garbage or truncated trailers to guarantee strict 188-byte alignment starting with sync byte 0x47.
     */
    private fun cleanTsStream(data: ByteArray): ByteArray {
        if (data.size < TS_PACKET_SIZE) return data
        var startOffset = 0
        while (startOffset < data.size && data[startOffset] != TS_SYNC_BYTE) {
            startOffset++
        }
        if (startOffset >= data.size) return data

        val available = data.size - startOffset
        val alignedLength = (available / TS_PACKET_SIZE) * TS_PACKET_SIZE
        if (alignedLength <= 0) return data

        return if (startOffset == 0 && alignedLength == data.size) {
            data
        } else {
            data.copyOfRange(startOffset, startOffset + alignedLength)
        }
    }

    /**
     * Parses HLS Master or Media Playlists, selecting the requested quality & audio track.
     */
    private suspend fun parseHlsPlaylist(
        playlistUrl: String,
        headers: Map<String, String>,
        preferredQuality: String? = null,
        preferredAudio: String? = null
    ): HlsMediaInfo = withContext(Dispatchers.IO) {
        val content = downloadString(playlistUrl, headers)
        val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }

        val isMaster = lines.any { it.startsWith("#EXT-X-STREAM-INF") }
        if (isMaster) {
            val variants = mutableListOf<VariantEntry>()
            val audioTracks = mutableListOf<AudioTrackEntry>()

            for (i in lines.indices) {
                val line = lines[i]
                if (line.startsWith("#EXT-X-STREAM-INF")) {
                    val bwMatch = Regex("BANDWIDTH=(\\d+)").find(line)
                    val bw = bwMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

                    val resMatch = Regex("RESOLUTION=(\\d+)x(\\d+)").find(line)
                    val resH = resMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

                    val nameMatch = Regex("NAME=\"([^\"]+)\"").find(line)
                    val name = nameMatch?.groupValues?.get(1) ?: ""

                    val audioGroupMatch = Regex("AUDIO=\"([^\"]+)\"").find(line)
                    val audioGroup = audioGroupMatch?.groupValues?.get(1)

                    val uriLine = lines.getOrNull(i + 1)
                    if (uriLine != null && !uriLine.startsWith("#")) {
                        variants.add(
                            VariantEntry(
                                uri = resolveUrl(playlistUrl, uriLine),
                                bandwidth = bw,
                                resolutionHeight = resH,
                                name = name,
                                audioGroupId = audioGroup
                            )
                        )
                    }
                } else if (line.startsWith("#EXT-X-MEDIA:TYPE=AUDIO")) {
                    val uriMatch = Regex("URI=\"([^\"]+)\"").find(line)
                    val uri = uriMatch?.groupValues?.get(1)
                    if (!uri.isNullOrBlank()) {
                        val groupMatch = Regex("GROUP-ID=\"([^\"]+)\"").find(line)
                        val groupId = groupMatch?.groupValues?.get(1) ?: ""

                        val nameMatch = Regex("NAME=\"([^\"]+)\"").find(line)
                        val name = nameMatch?.groupValues?.get(1) ?: ""

                        val langMatch = Regex("LANGUAGE=\"([^\"]+)\"").find(line)
                        val lang = langMatch?.groupValues?.get(1)

                        val defMatch = Regex("DEFAULT=(YES|NO)").find(line)
                        val isDefault = defMatch?.groupValues?.get(1)?.equals("YES", ignoreCase = true) == true

                        audioTracks.add(
                            AudioTrackEntry(
                                uri = resolveUrl(playlistUrl, uri),
                                groupId = groupId,
                                name = name,
                                language = lang,
                                isDefault = isDefault
                            )
                        )
                    }
                }
            }

            // Quality matching
            val targetHeight = when {
                preferredQuality.isNullOrBlank() || preferredQuality.equals("Auto", ignoreCase = true) -> null
                preferredQuality.contains("1080") -> 1080
                preferredQuality.contains("720") -> 720
                preferredQuality.contains("480") -> 480
                preferredQuality.contains("360") -> 360
                else -> preferredQuality.filter { it.isDigit() }.toIntOrNull()
            }

            val chosenVariant = if (targetHeight != null && variants.isNotEmpty()) {
                variants.firstOrNull { it.resolutionHeight == targetHeight || it.name.contains("$targetHeight") }
                    ?: variants.minByOrNull { abs(it.resolutionHeight - targetHeight) }
                    ?: variants.maxByOrNull { it.bandwidth }
                    ?: variants.first()
            } else {
                variants.maxByOrNull { it.bandwidth } ?: variants.firstOrNull()
            }

            val tracksToDownload = mutableListOf<AudioTrackEntry>()
            if (chosenVariant != null && audioTracks.isNotEmpty()) {
                val groupTracks = if (chosenVariant.audioGroupId != null) {
                    audioTracks.filter { it.groupId == chosenVariant.audioGroupId }
                } else audioTracks

                val effectiveTracks = if (groupTracks.isNotEmpty()) groupTracks else audioTracks

                val isMultiRequested = preferredAudio?.contains("MULTI", ignoreCase = true) == true
                if (isMultiRequested) {
                    // Include all distinct audio tracks ONLY when Multi is explicitly requested
                    val distinctTracks = effectiveTracks.distinctBy { it.uri }
                    tracksToDownload.addAll(distinctTracks)
                } else {
                    val singleTrack = when {
                        preferredAudio?.contains("DUB", ignoreCase = true) == true || preferredAudio?.contains("English", ignoreCase = true) == true -> {
                            effectiveTracks.firstOrNull { it.name.contains("Dub", true) || it.name.contains("English", true) || it.language?.startsWith("en", true) == true }
                                ?: effectiveTracks.firstOrNull { it.isDefault }
                                ?: effectiveTracks.firstOrNull()
                        }
                        preferredAudio?.contains("SUB", ignoreCase = true) == true || preferredAudio?.contains("Japanese", ignoreCase = true) == true -> {
                            effectiveTracks.firstOrNull { it.name.contains("Sub", true) || it.name.contains("Japanese", true) || it.language?.startsWith("ja", true) == true }
                                ?: effectiveTracks.firstOrNull { it.isDefault }
                                ?: effectiveTracks.firstOrNull()
                        }
                        else -> effectiveTracks.firstOrNull { it.isDefault } ?: effectiveTracks.firstOrNull()
                    }
                    if (singleTrack != null) {
                        tracksToDownload.add(singleTrack)
                    }
                }
            }

            val videoMediaInfo = if (chosenVariant != null) {
                parseHlsMediaPlaylist(chosenVariant.uri, headers)
            } else {
                parseHlsMediaPlaylist(playlistUrl, headers)
            }

            val parsedAudioTracks = mutableListOf<HlsAudioTrackInfo>()
            tracksToDownload.forEachIndexed { idx, track ->
                if (track.uri != chosenVariant?.uri) {
                    try {
                        val aMedia = parseHlsMediaPlaylist(track.uri, headers, isAudio = true)
                        parsedAudioTracks.add(
                            HlsAudioTrackInfo(
                                index = idx,
                                name = track.name.ifBlank { "Audio $idx" },
                                groupId = track.groupId.ifBlank { "audio" },
                                language = track.language,
                                isDefault = track.isDefault || (idx == 0 && tracksToDownload.none { it.isDefault }),
                                mediaInfo = aMedia
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse audio playlist ${track.uri}: ${e.message}")
                    }
                }
            }

            return@withContext videoMediaInfo.copy(audioTracks = parsedAudioTracks)
        }

        // Already a media playlist
        parseHlsMediaPlaylist(playlistUrl, headers)
    }

    /**
     * Parses a single HLS Media Playlist into segment metadata.
     */
    private suspend fun parseHlsMediaPlaylist(
        playlistUrl: String,
        headers: Map<String, String>,
        isAudio: Boolean = false
    ): HlsMediaInfo = withContext(Dispatchers.IO) {
        val content = downloadString(playlistUrl, headers)
        val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }

        val segments = mutableListOf<HlsSegment>()
        var currentKeyUri: String? = null
        var currentKeyIv: ByteArray? = null
        var currentDuration = 0.0
        var totalDuration = 0.0
        var currentByteRangeOffset: Long? = null
        var currentByteRangeLength: Long? = null
        var lastByteRangeEnd: Long = 0L
        var initSegmentUri: String? = null
        var isFragmentedMp4 = false
        var isEncrypted = false
        var mediaSequence = 0L

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> {
                    mediaSequence = line.removePrefix("#EXT-X-MEDIA-SEQUENCE:").trim().toLongOrNull() ?: 0L
                }
                line.startsWith("#EXT-X-MAP:") -> {
                    isFragmentedMp4 = true
                    val uriMatch = Regex("URI=\"([^\"]+)\"").find(line)
                    if (uriMatch != null) {
                        initSegmentUri = resolveUrl(playlistUrl, uriMatch.groupValues[1])
                    }
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    isEncrypted = true
                    val methodMatch = Regex("METHOD=([A-Z0-9-]+)").find(line)
                    val method = methodMatch?.groupValues?.get(1) ?: "NONE"
                    if (method == "AES-128") {
                        val uriMatch = Regex("URI=\"([^\"]+)\"").find(line)
                        if (uriMatch != null) {
                            currentKeyUri = resolveUrl(playlistUrl, uriMatch.groupValues[1])
                        }
                        val ivMatch = Regex("IV=0x([0-9a-fA-F]+)").find(line)
                        if (ivMatch != null) {
                            currentKeyIv = hexStringToByteArray(ivMatch.groupValues[1])
                        } else {
                            currentKeyIv = null
                        }
                    } else if (method == "NONE") {
                        currentKeyUri = null
                        currentKeyIv = null
                    }
                }
                line.startsWith("#EXTINF:") -> {
                    val durStr = line.removePrefix("#EXTINF:").substringBefore(",").trim()
                    currentDuration = durStr.toDoubleOrNull() ?: 0.0
                    totalDuration += currentDuration
                }
                line.startsWith("#EXT-X-BYTERANGE:") -> {
                    val rangeStr = line.removePrefix("#EXT-X-BYTERANGE:").trim()
                    if (rangeStr.contains("@")) {
                        currentByteRangeLength = rangeStr.substringBefore("@").toLongOrNull()
                        currentByteRangeOffset = rangeStr.substringAfter("@").toLongOrNull()
                        if (currentByteRangeLength != null && currentByteRangeOffset != null) {
                            lastByteRangeEnd = currentByteRangeOffset + currentByteRangeLength
                        }
                    } else {
                        currentByteRangeLength = rangeStr.toLongOrNull()
                        currentByteRangeOffset = lastByteRangeEnd
                        if (currentByteRangeLength != null) {
                            lastByteRangeEnd += currentByteRangeLength
                        }
                    }
                }
                !line.startsWith("#") && line.isNotBlank() -> {
                    val segUri = resolveUrl(playlistUrl, line)
                    if (segUri.endsWith(".m4s", ignoreCase = true) || segUri.contains(".m4s?", ignoreCase = true)) {
                        isFragmentedMp4 = true
                    }
                    val seqNum = mediaSequence + segments.size
                    segments.add(
                        HlsSegment(
                            index = segments.size,
                            uri = segUri,
                            durationSec = currentDuration,
                            byteRangeOffset = currentByteRangeOffset,
                            byteRangeLength = currentByteRangeLength,
                            keyUri = currentKeyUri,
                            keyIv = currentKeyIv,
                            sequenceNumber = seqNum,
                            isAudio = isAudio
                        )
                    )
                    currentByteRangeOffset = null
                    currentByteRangeLength = null
                }
            }
        }

        HlsMediaInfo(
            isMaster = false,
            targetUrl = playlistUrl,
            initSegmentUri = initSegmentUri,
            isFragmentedMp4 = isFragmentedMp4,
            segments = segments,
            isEncrypted = isEncrypted,
            totalDurationSec = totalDuration
        )
    }

    private suspend fun downloadSegmentBytesWithRetry(
        segment: HlsSegment,
        headers: Map<String, String>,
        isCancelled: () -> Boolean,
        maxRetries: Int = 8
    ): ByteArray = withContext(Dispatchers.IO) {
        var lastErr: Exception? = null
        for (attempt in 1..maxRetries) {
            if (isCancelled()) throw CancellationException("Cancelled")
            try {
                val reqBuilder = Request.Builder()
                    .url(segment.uri)
                    .header("User-Agent", headers["User-Agent"] ?: DEFAULT_UA)

                headers.forEach { (k, v) ->
                    if (!k.equals("User-Agent", ignoreCase = true)) {
                        reqBuilder.header(k, v)
                    }
                }

                if (segment.byteRangeLength != null && segment.byteRangeOffset != null) {
                    val end = segment.byteRangeOffset + segment.byteRangeLength - 1
                    reqBuilder.header("Range", "bytes=${segment.byteRangeOffset}-$end")
                }

                client.newCall(reqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code} downloading segment ${segment.index}")
                    }
                    val body = response.body ?: throw IllegalStateException("Empty segment body")
                    return@withContext body.bytes()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastErr = e
                Log.w(TAG, "Segment ${segment.index} attempt $attempt/$maxRetries failed: ${e.message}")
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(1000L * attempt)
                }
            }
        }
        throw lastErr ?: IllegalStateException("Failed to download segment ${segment.index}")
    }

    private suspend fun downloadDirectFile(
        url: String,
        destFile: File,
        headers: Map<String, String>,
        isCancelled: () -> Boolean,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long, speedFormatted: String, completedSegments: Int, totalSegments: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        destFile.parentFile?.mkdirs()
        val tempFile = File(destFile.parentFile, "${destFile.name}.tmp")
        if (tempFile.exists()) tempFile.delete()

        // Build header strategies:
        // Native MovieBox requires ExoPlayer UA and NO Referer/Origin (browser UA gets 428 Precondition Required).
        // Web MovieBox requires browser UA with Referer: https://123moviesfree.club/
        val host = try { URI(url).host ?: "" } catch (_: Exception) { "" }
        val isWebMb = url.contains("bcdnxw") || url.contains("hakunaymatata.com/bt/")
        val isNativeMb = (host.contains("hakunaymatata") || host.contains("aoneroom") || host.contains("febbox") || url.contains("hakunaymatata") || url.contains("aoneroom")) && !isWebMb

        val strategies = mutableListOf<Map<String, String>>()
        if (isNativeMb) {
            strategies.add(mapOf("User-Agent" to "ExoPlayerLib/2.19.1", "Accept" to "*/*"))
            strategies.add(mapOf("User-Agent" to "ExoPlayerLib/2.18.5", "Accept" to "*/*"))
            strategies.add(mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36", "Referer" to "https://123moviesfree.club/", "Accept" to "*/*"))
        } else if (isWebMb) {
            strategies.add(mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36", "Referer" to "https://123moviesfree.club/", "Accept" to "*/*"))
            strategies.add(mapOf("User-Agent" to "ExoPlayerLib/2.19.1", "Accept" to "*/*"))
        } else {
            if (headers.isNotEmpty()) {
                strategies.add(headers)
            }
            strategies.add(mapOf("User-Agent" to "ExoPlayerLib/2.19.1", "Accept" to "*/*"))
            strategies.add(mapOf("User-Agent" to DEFAULT_UA, "Accept" to "*/*"))
        }

        var lastException: Exception? = null
        for (strategyHeaders in strategies) {
            if (isCancelled()) throw CancellationException("Download cancelled")
            if (tempFile.exists()) tempFile.delete()

            val reqBuilder = Request.Builder().url(url)
            strategyHeaders.forEach { (k, v) ->
                reqBuilder.header(k, v)
            }

            var successful = false
            try {
                client.newCall(reqBuilder.build()).execute().use { response ->
                    if (response.code == 428 || response.code == 429 || response.code == 403 || response.code == 401) {
                        Log.w(TAG, "Direct download got HTTP ${response.code} with UA '${strategyHeaders["User-Agent"]}', trying next strategy...")
                        lastException = IllegalStateException("HTTP ${response.code} downloading video")
                        return@use
                    }
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code} downloading video")
                    }

                    val body = response.body ?: throw IllegalStateException("Empty body")
                    val rawContentLength = body.contentLength()
                    val totalBytes = if (rawContentLength > 0L) rawContentLength else -1L

                    var downloadedBytes = 0L
                    val startTime = System.currentTimeMillis()
                    var lastReportTime = 0L
                    val buffer = ByteArray(64 * 1024)

                    FileOutputStream(tempFile).use { fos ->
                        body.byteStream().use { input ->
                            while (true) {
                                if (isCancelled()) throw CancellationException("Download cancelled")
                                val read = input.read(buffer)
                                if (read == -1) break
                                fos.write(buffer, 0, read)
                                downloadedBytes += read

                                val now = System.currentTimeMillis()
                                if (now - lastReportTime > 200) {
                                    lastReportTime = now
                                    val elapsedSec = (now - startTime) / 1000.0
                                    val speed = if (elapsedSec > 0.5) (downloadedBytes / elapsedSec).toLong() else 0L
                                    val rawProg2 = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 0.99f) else 0.05f
                                    val progress = if (rawProg2.isNaN() || !rawProg2.isFinite()) 0.05f else rawProg2
                                    onProgress(progress, downloadedBytes, if (totalBytes > 0L) totalBytes else 0L, formatSpeed(speed), 1, 1)
                                }
                            }
                            fos.flush()
                        }
                    }

                    successful = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Direct download attempt failed: ${e.message}")
            }

            if (successful && tempFile.exists() && tempFile.length() > 0L) {
                if (destFile.exists()) destFile.delete()
                val renamed = tempFile.renameTo(destFile)
                if (!renamed) {
                    tempFile.copyTo(destFile, overwrite = true)
                    tempFile.delete()
                }
                val finalLen = destFile.length()
                onProgress(1.0f, finalLen, finalLen, "Completed", 1, 1)
                return@withContext
            }
        }

        throw lastException ?: IllegalStateException("Failed to download video file")
    }

    private fun decryptAes128(
        encryptedData: ByteArray,
        key: ByteArray,
        iv: ByteArray?,
        sequenceNumber: Long
    ): ByteArray {
        val effectiveIv = iv ?: ByteBufferSequenceToIv(sequenceNumber)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(effectiveIv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(encryptedData)
    }

    private fun ByteBufferSequenceToIv(seq: Long): ByteArray {
        val iv = ByteArray(16)
        var temp = seq
        for (i in 15 downTo 8) {
            iv[i] = (temp and 0xFF).toByte()
            temp = temp shr 8
        }
        return iv
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private suspend fun downloadString(url: String, headers: Map<String, String>): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", headers["User-Agent"] ?: DEFAULT_UA)
        headers.forEach { (k, v) ->
            if (!k.equals("User-Agent", ignoreCase = true)) req.header(k, v)
        }
        client.newCall(req.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code} from $url")
            resp.body?.string() ?: ""
        }
    }

    private suspend fun downloadBytes(url: String, headers: Map<String, String>): ByteArray = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", headers["User-Agent"] ?: DEFAULT_UA)
        headers.forEach { (k, v) ->
            if (!k.equals("User-Agent", ignoreCase = true)) req.header(k, v)
        }
        client.newCall(req.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code} from $url")
            resp.body?.bytes() ?: ByteArray(0)
        }
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl
        }
        return try {
            val base = URI(baseUrl)
            base.resolve(relativeUrl).toString()
        } catch (_: Exception) {
            if (relativeUrl.startsWith("/")) {
                val uri = try { android.net.Uri.parse(baseUrl) } catch (_: Exception) { null }
                if (uri != null && !uri.scheme.isNullOrBlank() && !uri.host.isNullOrBlank()) {
                    val portStr = if (uri.port != -1) ":${uri.port}" else ""
                    "${uri.scheme}://${uri.host}$portStr$relativeUrl"
                } else {
                    val idx = baseUrl.indexOf("://")
                    if (idx != -1) {
                        val nextSlash = baseUrl.indexOf("/", idx + 3)
                        val hostPart = if (nextSlash != -1) baseUrl.substring(0, nextSlash) else baseUrl
                        "$hostPart$relativeUrl"
                    } else {
                        "$baseUrl$relativeUrl"
                    }
                }
            } else {
                val lastSlash = baseUrl.lastIndexOf('/')
                if (lastSlash != -1) {
                    baseUrl.substring(0, lastSlash + 1) + relativeUrl
                } else {
                    "$baseUrl/$relativeUrl"
                }
            }
        }
    }

    private fun generateOfflineM3u8(outputDir: File, mediaInfo: HlsMediaInfo, isFmp4: Boolean) {
        try {
            val segmentsDir = File(outputDir, "segments")
            val audioTracks = mediaInfo.audioTracks
            val ext = if (isFmp4) "m4s" else "ts"
            val hlsVersion = if (isFmp4) 7 else 3

            val videoInitFile = File(segmentsDir, "video_init.mp4")
            val hasVideoInit = isFmp4 && videoInitFile.exists() && videoInitFile.length() > 0

            if (audioTracks.isNotEmpty()) {
                // 1. Write video_playlist.m3u8
                val videoPlFile = File(outputDir, "video_playlist.m3u8")
                val vSb = StringBuilder()
                vSb.append("#EXTM3U\n#EXT-X-VERSION:$hlsVersion\n#EXT-X-TARGETDURATION:15\n#EXT-X-MEDIA-SEQUENCE:0\n#EXT-X-PLAYLIST-TYPE:VOD\n")
                if (hasVideoInit) {
                    vSb.append("#EXT-X-MAP:URI=\"segments/video_init.mp4\"\n")
                }
                for (seg in mediaInfo.segments) {
                    vSb.append("#EXTINF:${seg.durationSec},\nsegments/video_seg_${String.format("%05d", seg.index)}.$ext\n")
                }
                vSb.append("#EXT-X-ENDLIST\n")
                videoPlFile.writeText(vSb.toString())

                // 2. Write audio_playlist_${trackIdx}.m3u8 for each track
                for (track in audioTracks) {
                    val trackIdx = track.index
                    val isTrackFmp4 = track.mediaInfo.isFragmentedMp4 || track.mediaInfo.initSegmentUri != null || isFmp4
                    val audioExt = if (isTrackFmp4) "m4s" else "aac"
                    val audioInitFile = File(segmentsDir, "audio_${trackIdx}_init.mp4")
                    val hasAudioInit = isTrackFmp4 && audioInitFile.exists() && audioInitFile.length() > 0

                    val audioPlFile = File(outputDir, "audio_playlist_${trackIdx}.m3u8")
                    val aSb = StringBuilder()
                    aSb.append("#EXTM3U\n#EXT-X-VERSION:${if (isTrackFmp4) 7 else 3}\n#EXT-X-TARGETDURATION:15\n#EXT-X-MEDIA-SEQUENCE:0\n#EXT-X-PLAYLIST-TYPE:VOD\n")
                    if (hasAudioInit) {
                        aSb.append("#EXT-X-MAP:URI=\"segments/audio_${trackIdx}_init.mp4\"\n")
                    } else if (isTrackFmp4 && hasVideoInit) {
                        aSb.append("#EXT-X-MAP:URI=\"segments/video_init.mp4\"\n")
                    }
                    for (seg in track.mediaInfo.segments) {
                        aSb.append("#EXTINF:${seg.durationSec},\nsegments/audio_${trackIdx}_seg_${String.format("%05d", seg.index)}.$audioExt\n")
                    }
                    aSb.append("#EXT-X-ENDLIST\n")
                    audioPlFile.writeText(aSb.toString())
                }

                // 3. Write master playlist.m3u8 linking video and all audio tracks
                val masterPlFile = File(outputDir, "playlist.m3u8")
                val mSb = StringBuilder()
                mSb.append("#EXTM3U\n#EXT-X-VERSION:$hlsVersion\n")
                for (track in audioTracks) {
                    val trackIdx = track.index
                    val defStr = if (audioTracks.size == 1 || track.isDefault) "YES" else "NO"
                    val langStr = if (!track.language.isNullOrBlank()) "LANGUAGE=\"${track.language}\"," else ""
                    val safeName = track.name.replace("\"", "")
                    mSb.append("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"$safeName\",DEFAULT=$defStr,AUTOSELECT=YES,${langStr}URI=\"audio_playlist_${trackIdx}.m3u8\"\n")
                }
                mSb.append("#EXT-X-STREAM-INF:BANDWIDTH=3000000,AUDIO=\"audio\"\n")
                mSb.append("video_playlist.m3u8\n")
                masterPlFile.writeText(mSb.toString())
            } else {
                val playlistFile = File(outputDir, "playlist.m3u8")
                val sb = StringBuilder()
                sb.append("#EXTM3U\n")
                sb.append("#EXT-X-VERSION:$hlsVersion\n")
                sb.append("#EXT-X-TARGETDURATION:15\n")
                sb.append("#EXT-X-MEDIA-SEQUENCE:0\n")
                sb.append("#EXT-X-PLAYLIST-TYPE:VOD\n")
                if (hasVideoInit) {
                    sb.append("#EXT-X-MAP:URI=\"segments/video_init.mp4\"\n")
                }

                for (seg in mediaInfo.segments) {
                    sb.append("#EXTINF:${seg.durationSec},\n")
                    sb.append("segments/video_seg_${String.format("%05d", seg.index)}.$ext\n")
                }
                sb.append("#EXT-X-ENDLIST\n")
                playlistFile.writeText(sb.toString())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write offline playlist.m3u8: ${e.message}")
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return ""
        val kb = bytesPerSec / 1024.0
        if (kb < 1024) return "${String.format("%.1f", kb)} KB/s"
        val mb = kb / 1024.0
        return "${String.format("%.1f", mb)} MB/s"
    }
}
