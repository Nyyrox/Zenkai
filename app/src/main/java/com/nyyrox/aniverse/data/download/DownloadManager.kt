package com.nyyrox.aniverse.data.download

import android.content.Context
import android.util.Log
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.Provider
import com.nyyrox.aniverse.data.extractor.DirectStreamResolver
import com.nyyrox.aniverse.data.extractor.ServerType
import com.nyyrox.aniverse.player.StreamSniffer
import com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver
import com.nyyrox.aniverse.data.mapping.AnimeMappingRepository
import com.nyyrox.aniverse.data.mapping.AniBridgeResolver
import com.nyyrox.aniverse.data.manga.MangaChapter
import com.nyyrox.aniverse.data.manga.MangaRepository
import com.nyyrox.aniverse.data.novel.INovelSourceRepository
import com.nyyrox.aniverse.data.novel.NovelChapter
import com.nyyrox.aniverse.data.novel.NovelChapterContent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import android.net.Uri

class DownloadManager(private val context: Context) {
    companion object {
        @Volatile
        var instance: DownloadManager? = null
    }

    // Safety net: no background download/IO error can ever crash the app
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, e -> Log.e("DownloadManager", "Uncaught: ${e.message}", e) }
    )
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val animeDownloader = AnimeStreamDownloader(client)
    private val animeCancellationTokens = ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicBoolean>()
    // Folder the user picked for this exact download (kept across Retry, dropped on cancel/success)
    private val exportTargets = ConcurrentHashMap<String, Uri>()

    @Volatile
    private var currentMaxConcurrent = DownloadPreferences.getMaxConcurrentDownloads(context)
    @Volatile
    private var animeDownloadSemaphore = Semaphore(currentMaxConcurrent)

    private fun getDownloadSemaphore(): Semaphore {
        val max = DownloadPreferences.getMaxConcurrentDownloads(context)
        if (max != currentMaxConcurrent) {
            synchronized(this) {
                if (max != currentMaxConcurrent) {
                    currentMaxConcurrent = max
                    animeDownloadSemaphore = Semaphore(max)
                }
            }
        }
        return animeDownloadSemaphore
    }

    private val fallbackAnimeDir: File
        get() = File(context.filesDir, "downloads/anime").apply { if (!exists()) mkdirs() }

    // Real write probe: mkdirs()/canWrite() lie on Android 11+ scoped storage
    private fun isWritableDir(dir: File): Boolean = try {
        (dir.exists() || dir.mkdirs()) && File(dir, ".probe").run { writeText("ok"); delete() }
    } catch (_: Exception) { false }

    // Chosen folder if we can really write to it, otherwise app-private storage (never crashes)
    val primaryAnimeDir: File
        get() {
            val chosen = try { DownloadPreferences.getAnimeDownloadDir(context) } catch (_: Exception) { null }
            if (chosen != null && isWritableDir(chosen)) return chosen
            Log.w("DownloadManager", "Chosen download folder not writable (${chosen?.absolutePath}); using app storage")
            return fallbackAnimeDir
        }

    // Always includes the fallback dir so fallback downloads still list, play and delete
    val allAnimeDirs: List<File>
        get() {
            val dirs = try { DownloadPreferences.getAllAnimeDownloadDirs(context) } catch (_: Exception) { emptyList() }
            val fb = fallbackAnimeDir
            return if (dirs.any { it.absolutePath == fb.absolutePath }) dirs else dirs + fb
        }

    private val baseNovelDir: File
        get() = File(context.filesDir, "downloads/novels").apply { if (!exists()) mkdirs() }

    private val baseMangaDir: File
        get() = File(context.filesDir, "downloads/manga").apply { if (!exists()) mkdirs() }

    private val _animeTasks = MutableStateFlow<Map<String, AnimeDownloadTask>>(emptyMap())
    val animeTasks: StateFlow<Map<String, AnimeDownloadTask>> = _animeTasks.asStateFlow()

    private val _novelTasks = MutableStateFlow<Map<String, NovelDownloadTask>>(emptyMap())
    val novelTasks: StateFlow<Map<String, NovelDownloadTask>> = _novelTasks.asStateFlow()

    private val _mangaTasks = MutableStateFlow<Map<String, MangaDownloadTask>>(emptyMap())
    val mangaTasks: StateFlow<Map<String, MangaDownloadTask>> = _mangaTasks.asStateFlow()

    val tasks: StateFlow<List<DownloadTaskInfo>> = combine(_animeTasks, _novelTasks, _mangaTasks) { aMap, nMap, mMap ->
        val list = mutableListOf<DownloadTaskInfo>()
        aMap.values.forEach {
            list.add(
                DownloadTaskInfo(
                    type = DownloadType.ANIME,
                    itemId = it.animeId.toString(),
                    chapterId = it.episode.toString(),
                    title = it.episodeTitle ?: "Episode ${it.episode}",
                    itemTitle = it.animeTitle,
                    cover = it.animeCover,
                    status = it.status,
                    progress = it.progress,
                    downloadedCount = it.downloadedSegments,
                    totalCount = it.totalSegments,
                    speed = it.speedFormatted,
                    error = it.error,
                    providerId = it.providerId,
                    providerName = it.providerName,
                    audio = it.audio,
                    quality = it.quality,
                    downloadedBytes = it.downloadedBytes,
                    totalBytes = it.totalBytes
                )
            )
        }
        nMap.values.forEach {
            list.add(
                DownloadTaskInfo(
                    type = DownloadType.NOVEL,
                    itemId = it.novelSlug,
                    chapterId = it.chapterSlug,
                    title = it.chapterTitle,
                    itemTitle = it.novelTitle,
                    cover = it.novelCover,
                    status = it.status,
                    progress = it.progress,
                    downloadedCount = if (it.status == DownloadStatus.COMPLETED) 1 else 0,
                    totalCount = 1,
                    error = it.error
                )
            )
        }
        mMap.values.forEach {
            list.add(
                DownloadTaskInfo(
                    type = DownloadType.MANGA,
                    itemId = it.mangaId,
                    chapterId = it.chapterId,
                    title = it.chapterTitle ?: "Chapter ${it.chapterNumber}",
                    itemTitle = it.mangaTitle,
                    cover = it.mangaPoster,
                    status = it.status,
                    progress = it.progress,
                    downloadedCount = it.downloadedPages,
                    totalCount = it.totalPages,
                    error = it.error
                )
            )
        }
        list
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun cancelTask(type: DownloadType, itemId: String, chapterId: String) {
        val taskKey = "$itemId:$chapterId"
        when (type) {
            DownloadType.ANIME -> {
                animeCancellationTokens[taskKey]?.set(true)
                animeCancellationTokens.remove(taskKey)
                exportTargets.remove(taskKey)
                _animeTasks.value = _animeTasks.value - taskKey
                DownloadNotificationHelper.cancelNotification(context, taskKey)
            }
            DownloadType.NOVEL -> {
                _novelTasks.value = _novelTasks.value - taskKey
            }
            DownloadType.MANGA -> {
                _mangaTasks.value = _mangaTasks.value - taskKey
            }
        }
    }

    fun clearCompletedTasks() {
        _animeTasks.value = _animeTasks.value.filterValues { it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED }
        _novelTasks.value = _novelTasks.value.filterValues { it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED }
        _mangaTasks.value = _mangaTasks.value.filterValues { it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED }
    }

    private val _downloadedAnime = MutableStateFlow<List<DownloadedAnime>>(emptyList())
    val downloadedAnime: StateFlow<List<DownloadedAnime>> = _downloadedAnime.asStateFlow()

    private val _downloadedNovels = MutableStateFlow<List<DownloadedNovel>>(emptyList())
    val downloadedNovels: StateFlow<List<DownloadedNovel>> = _downloadedNovels.asStateFlow()

    private val _downloadedManga = MutableStateFlow<List<DownloadedManga>>(emptyList())
    val downloadedManga: StateFlow<List<DownloadedManga>> = _downloadedManga.asStateFlow()

    init {
        instance = this
        DownloadPreferences.init(context)
        refreshDownloadedLists()
    }

    fun refreshDownloadedLists() {
        scope.launch {
            _downloadedAnime.value = loadDownloadedAnimeFromDisk()
            _downloadedNovels.value = loadDownloadedNovelsFromDisk()
            _downloadedManga.value = loadDownloadedMangaFromDisk()
        }
    }

    // ========================================================================
    // 0. ANIME DOWNLOADS
    // ========================================================================

    fun isAnimeEpisodeDownloaded(animeId: Int, episode: Int): Boolean {
        for (baseDir in allAnimeDirs) {
            val epDir = File(baseDir, "$animeId/$episode")
            if (!epDir.exists() || !epDir.isDirectory) continue
            val videoFile = File(epDir, "playlist.m3u8").takeIf { it.exists() && it.length() > 0 }
                ?: File(epDir, "video.mp4").takeIf { it.exists() && it.length() > 1024 }
                ?: File(epDir, "video.ts").takeIf { it.exists() && it.length() > 1024 }
            if (videoFile != null) return true
        }
        return false
    }

    fun getDownloadedAnimeEpisode(animeId: Int, episode: Int): DownloadedAnimeEpisode? {
        for (baseDir in allAnimeDirs) {
            val epDir = File(baseDir, "$animeId/$episode")
            if (!epDir.exists() || !epDir.isDirectory) continue
            val videoFile = File(epDir, "playlist.m3u8").takeIf { it.exists() && it.length() > 0 }
                ?: File(epDir, "video.mp4").takeIf { it.exists() && it.length() > 1024 }
                ?: File(epDir, "video.ts").takeIf { it.exists() && it.length() > 1024 }
                ?: continue

            val metaFile = File(epDir, "meta.json")
            var epTitle: String? = null
            var animeTitle = ""
            var animeCover = ""
            var provId = ""
            var provName = ""
            var downloadedAt = videoFile.lastModified()
            var metaFileSize = 0L

            val parsedSubtitles = mutableListOf<com.nyyrox.aniverse.data.subtitles.SubtitleTrack>()
            if (metaFile.exists()) {
                try {
                    val json = JSONObject(metaFile.readText())
                    epTitle = json.optString("episodeTitle").takeIf { it.isNotBlank() }
                    animeTitle = json.optString("animeTitle", "")
                    animeCover = json.optString("animeCover", "")
                    provId = json.optString("providerId", "")
                    provName = json.optString("providerName", "")
                    downloadedAt = json.optLong("downloadedAt", downloadedAt)
                    metaFileSize = json.optLong("fileSize", 0L)

                    val subArray = json.optJSONArray("subtitles")
                    if (subArray != null) {
                        for (i in 0 until subArray.length()) {
                            val sObj = subArray.optJSONObject(i) ?: continue
                            val sPath = sObj.optString("filePath", "")
                            val sFile = File(sPath)
                            if (sFile.exists() && sFile.length() > 0) {
                                parsedSubtitles.add(
                                    com.nyyrox.aniverse.data.subtitles.SubtitleTrack(
                                        id = sObj.optString("id", sFile.nameWithoutExtension),
                                        label = sObj.optString("label", "English"),
                                        language = sObj.optString("language", "English"),
                                        url = sFile.absolutePath,
                                        isDefault = sObj.optBoolean("isDefault", false),
                                        source = "Downloaded"
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Fallback: If no subtitles parsed from meta, scan epDir for existing .vtt, .srt, .ass files
            if (parsedSubtitles.isEmpty()) {
                val subFiles = epDir.listFiles { f ->
                    f.isFile && (f.name.endsWith(".vtt", ignoreCase = true) || f.name.endsWith(".srt", ignoreCase = true) || f.name.endsWith(".ass", ignoreCase = true))
                } ?: emptyArray()
                subFiles.forEach { f ->
                    val lang = f.nameWithoutExtension.removePrefix("sub_").replace("_", " ").ifBlank { "English" }
                    parsedSubtitles.add(
                        com.nyyrox.aniverse.data.subtitles.SubtitleTrack(
                            id = f.nameWithoutExtension,
                            label = lang,
                            language = lang,
                            url = f.absolutePath,
                            isDefault = lang.contains("English", ignoreCase = true),
                            source = "Downloaded"
                        )
                    )
                }
            }

            // Calculate actual total size on disk for this episode (manifest + all segments + video files + subtitles)
            val directorySize = try {
                epDir.walkTopDown().filter { it.isFile && it.name != "meta.json" }.sumOf { it.length() }
            } catch (_: Exception) { 0L }

            val actualSize = when {
                directorySize > 0L -> directorySize
                metaFileSize > 0L -> metaFileSize
                else -> videoFile.length()
            }

            return DownloadedAnimeEpisode(
                animeId = animeId,
                episode = episode,
                episodeTitle = epTitle,
                animeTitle = animeTitle,
                animeCover = animeCover,
                providerId = provId,
                providerName = provName,
                filePath = videoFile.absolutePath,
                localUri = videoFile.absolutePath,
                sizeBytes = actualSize,
                downloadedAt = downloadedAt,
                subtitles = parsedSubtitles
            )
        }
        return null
    }

    private fun getCandidateProviders(
        anime: Anime,
        episode: Int,
        audio: String,
        selectedProvider: Provider?
    ): List<Provider> {
        val targetAudio = audio.uppercase()
        val isDub = targetAudio == "DUB"
        val isMulti = targetAudio == "MULTI"

                // 1. Embed servers
        val embedServers = when {
            isDub -> com.nyyrox.aniverse.data.DUB_PROVIDERS.filter { it.isEmbedOnly }
            isMulti -> com.nyyrox.aniverse.data.MULTI_PROVIDERS.filter { it.isEmbedOnly }
            else -> com.nyyrox.aniverse.data.SUB_PROVIDERS.filter { it.isEmbedOnly }
        }

        // 2. Direct HD servers
        val directServers = when {
            isDub -> com.nyyrox.aniverse.data.DUB_PROVIDERS.filter { it.isDirectStream }
            isMulti -> com.nyyrox.aniverse.data.MULTI_PROVIDERS.filter { it.isDirectStream }
            else -> com.nyyrox.aniverse.data.SUB_PROVIDERS.filter { it.isDirectStream }
        }

        // Rule: First try direct HD servers, then embed servers via stream sniffer
        val fullList = directServers + embedServers

        if (selectedProvider != null && !selectedProvider.id.startsWith("auto-all")) {
            val prioritized = mutableListOf<Provider>()
            val matching = fullList.find { it.id == selectedProvider.id } ?: selectedProvider
            prioritized.add(matching)
            fullList.filter { it.id != matching.id }.forEach { prioritized.add(it) }
            return prioritized
        }

        return fullList
    }

    fun downloadAnimeEpisode(
        anime: Anime,
        episode: Int,
        provider: Provider? = null,
        episodeTitle: String? = null,
        quality: String = "Auto HD",
        audio: String = "Default",
        exportTo: Uri? = null
    ) {
        val taskKey = "${anime.id}:$episode"
        if (exportTo != null) exportTargets[taskKey] = exportTo
        if (isAnimeEpisodeDownloaded(anime.id, episode)) {
            // Already in the app: don't silently do nothing when the user asked to save it outside.
            val target = exportTargets.remove(taskKey) ?: exportTo
            val wantsOutside = target != null ||
                DownloadPreferences.settings.value.storageLocation == DownloadStorageLocation.EXTERNAL_PUBLIC
            if (wantsOutside) exportExistingEpisode(anime.id, episode, anime.title, target)
            return
        }

        val existingTask = _animeTasks.value[taskKey]
        if (existingTask != null && (existingTask.status == DownloadStatus.DOWNLOADING || existingTask.status == DownloadStatus.QUEUED || existingTask.status == DownloadStatus.COMPLETED)) {
            return
        }

        val cancelToken = java.util.concurrent.atomic.AtomicBoolean(false)
        animeCancellationTokens[taskKey] = cancelToken

        val isDub = audio.contains("DUB", ignoreCase = true)
        val audioMode = if (isDub) "dub" else "sub"
        val effectiveProvider = provider
            ?: DirectStreamResolver.getDefaultDirectProviders(audioMode).firstOrNull()
            ?: Provider("auto-direct", "Best Server", "", "HD", isDirectStream = true)

        val initialTask = AnimeDownloadTask(
            animeId = anime.id,
            animeTitle = anime.title,
            animeCover = anime.banner ?: anime.cover,
            episode = episode,
            episodeTitle = episodeTitle ?: "Episode $episode",
            providerId = effectiveProvider.id,
            providerName = effectiveProvider.name,
            quality = quality,
            audio = audio,
            status = DownloadStatus.QUEUED,
            speedFormatted = "Queued...",
            progress = 0f
        )
        _animeTasks.value = _animeTasks.value + (taskKey to initialTask)
        DownloadNotificationHelper.updateProgressNotification(context, initialTask)

        // Any error outside the retry loop (storage, resolver, etc.) marks the task FAILED instead of crashing
        val failHandler = CoroutineExceptionHandler { _, e ->
            Log.e("DownloadManager", "Anime download failed: ${e.message}", e)
            animeCancellationTokens.remove(taskKey)
            val failed = initialTask.copy(
                status = DownloadStatus.FAILED,
                error = "Download failed: ${e.message ?: "Storage error"}. Tap Retry."
            )
            _animeTasks.value = _animeTasks.value + (taskKey to failed)
            try { DownloadNotificationHelper.showFailedNotification(context, failed, e.message ?: "Error") } catch (_: Exception) {}
        }

        scope.launch(failHandler) {
            if (cancelToken.get()) {
                DownloadNotificationHelper.cancelNotification(context, taskKey)
                return@launch
            }
            getDownloadSemaphore().withPermit {
                if (cancelToken.get()) {
                    DownloadNotificationHelper.cancelNotification(context, taskKey)
                    return@withPermit
                }

                val isMulti = audio.contains("MULTI", ignoreCase = true)
                var activeProvider = effectiveProvider
                val maxAttempts = 5
                var attempt = 0
                var downloadSucceeded = false
                var lastFailureReason = "No playable stream found"

                val animeFolder = File(primaryAnimeDir, anime.id.toString())
                val epFolder = File(animeFolder, episode.toString())
                check(epFolder.exists() || epFolder.mkdirs()) { "Can't create download folder: ${epFolder.absolutePath}" }

                // Save anime-level meta.json
                val animeMeta = File(animeFolder, "meta.json")
                val aJson = JSONObject().apply {
                    put("animeId", anime.id)
                    put("title", anime.title)
                    put("cover", anime.banner ?: anime.cover)
                    put("lastDownloadedAt", System.currentTimeMillis())
                }
                animeMeta.writeText(aJson.toString())

                while (attempt < maxAttempts && !downloadSucceeded) {
                    attempt++
                    if (cancelToken.get()) break

                    try {
                        val priorityServerTarget = when (attempt) {
                            1 -> null // Natural priority: 1st ACDN -> 2nd FlixCloud -> 3rd VidStream -> 4th MegaPlay
                            2 -> ServerType.FLIXCLOUD
                            3 -> ServerType.VIDSTREAM
                            4 -> ServerType.MEGAPLAY
                            else -> null
                        }

                        _animeTasks.value = _animeTasks.value + (taskKey to initialTask.copy(
                            providerId = activeProvider.id,
                            providerName = activeProvider.name,
                            speedFormatted = if (attempt > 1) "Retrying with next server ($attempt/$maxAttempts)..." else "Connecting to best server...",
                            status = DownloadStatus.DOWNLOADING
                        ))

                        // If retrying, clear stream cache so fresh manifest/tokens are obtained for this provider
                        if (attempt > 1) {
                            DirectStreamResolver.clearDirectStreamCache(activeProvider.id, anime.id, episode, audio)
                        }

                        // 1. Resolve raw playable stream URL & headers
                        var streamUrl: String? = null
                        var streamHeaders: Map<String, String> = emptyMap()
                        var streamSubtitles: List<com.nyyrox.aniverse.data.subtitles.SubtitleTrack> = emptyList()

                        // Try priority stream resolver first
                        val priorityResult = try {
                            DirectStreamResolver.resolvePriorityStream(
                                anilistId = anime.id,
                                episode = episode,
                                isDub = isDub,
                                animeTitle = anime.title,
                                preferredServer = priorityServerTarget
                            )
                        } catch (e: Exception) {
                            Log.w("DownloadManager", "Priority resolver error on attempt $attempt: ${e.message}")
                            null
                        }

                        if (priorityResult != null && !priorityResult.result.streamUrl.isNullOrBlank()) {
                            activeProvider = priorityResult.provider
                            streamUrl = priorityResult.result.streamUrl
                            streamHeaders = priorityResult.result.headers
                            streamSubtitles = priorityResult.result.subtitles
                        } else {
                            if (activeProvider.isDirectStream) {
                                val directResolved = DirectStreamResolver.resolveDirectStream(
                                    anilistId = anime.id,
                                    episode = episode,
                                    providerId = activeProvider.id,
                                    isDub = activeProvider.isDub || isDub,
                                    animeTitle = anime.title
                                )
                                if (directResolved != null) {
                                    streamUrl = directResolved.streamUrl
                                    streamHeaders = directResolved.headers
                                    streamSubtitles = directResolved.subtitles
                                }
                            } else {
                                // Embed server downloads via headless StreamSniffer
                                val isMovie = anime.format?.equals("MOVIE", ignoreCase = true) == true
                                val malId = anime.idMal ?: try {
                                    AniBridgeResolver.resolve(anime.id)?.malId
                                } catch (_: Exception) { null }
                                val tmdbId = try {
                                    AniBridgeResolver.resolve(anime.id)?.tmdbId
                                } catch (_: Exception) { null }

                                val embedUrl = if (activeProvider.id.startsWith("animesalt")) {
                                    val candidate = AnimeMappingRepository.resolveCandidate(anime.id, episode, anime.title)
                                    val saltId = candidate.saltId.ifBlank {
                                        AnimeSaltResolver.normalizeAnimeSaltTitle(anime.title)
                                    }
                                    val season = candidate.season ?: 1
                                    val finalEp = candidate.finalEp
                                    val slug = if (isMovie) saltId else "$saltId-${season}x$finalEp"
                                    val extracted = AnimeSaltResolver.extractServers(slug, isMovie)
                                    extracted.myStreamUrl ?: extracted.abyssUrl ?: "https://animesalt.cx/episode/$slug/"
                                } else {
                                    activeProvider.url(
                                        anilistId = anime.id,
                                        episode = episode,
                                        slug = null,
                                        malId = malId,
                                        tmdbId = tmdbId,
                                        isMovie = isMovie
                                    )
                                }

                                if (embedUrl.isNotBlank()) {
                                    _animeTasks.value = _animeTasks.value + (taskKey to initialTask.copy(
                                        providerId = activeProvider.id,
                                        providerName = activeProvider.name,
                                        speedFormatted = "Sniffing stream from ${activeProvider.name}...",
                                        status = DownloadStatus.DOWNLOADING
                                    ))

                                    val sniffed = try {
                                        StreamSniffer.sniff(
                                            context = context,
                                            embedUrl = embedUrl,
                                            timeoutMs = 15000L
                                        )
                                    } catch (_: Exception) { null }

                                    if (sniffed != null && sniffed.streamUrl.isNotBlank()) {
                                        streamUrl = sniffed.streamUrl
                                        streamHeaders = sniffed.headers
                                        streamSubtitles = sniffed.subtitles
                                    } else {
                                        lastFailureReason = "Could not sniff stream from ${activeProvider.name}"
                                    }
                                } else {
                                    lastFailureReason = "Could not resolve embed URL for ${activeProvider.name}"
                                }
                            }
                        }

                        if (streamUrl.isNullOrBlank()) {
                            lastFailureReason = "${activeProvider.name} returned no stream URL"
                            if (attempt < maxAttempts) {
                                kotlinx.coroutines.delay(2000L * attempt)
                                continue
                            } else {
                                break
                            }
                        }

                        _animeTasks.value = _animeTasks.value + (taskKey to initialTask.copy(
                            providerId = activeProvider.id,
                            providerName = activeProvider.name,
                            speedFormatted = if (attempt > 1) "Resuming from ${activeProvider.name}..." else "Downloading from ${activeProvider.name}...",
                            status = DownloadStatus.DOWNLOADING
                        ))

                        val finalFile = animeDownloader.downloadStream(
                            streamUrl = streamUrl,
                            outputDir = epFolder,
                            headers = streamHeaders,
                            preferredQuality = quality,
                            preferredAudio = audio,
                            isCancelled = { cancelToken.get() },
                            onProgress = { prog, dlBytes, totBytes, speedStr, compSegs, totSegs ->
                                val currentTask = initialTask.copy(
                                    providerId = activeProvider.id,
                                    providerName = activeProvider.name,
                                    progress = prog,
                                    downloadedBytes = dlBytes,
                                    totalBytes = totBytes,
                                    speedFormatted = speedStr,
                                    downloadedSegments = compSegs,
                                    totalSegments = totSegs,
                                    status = DownloadStatus.DOWNLOADING
                                )
                                _animeTasks.value = _animeTasks.value + (taskKey to currentTask)
                                DownloadNotificationHelper.updateProgressNotification(context, currentTask)
                            }
                        )

                    // Download subtitles/captions alongside the video file for offline playback
                    val downloadedSubtitles = mutableListOf<com.nyyrox.aniverse.data.subtitles.SubtitleTrack>()
                    try {
                        val preferredSubLang = com.nyyrox.aniverse.util.UserPreferences.getSubLanguageSync(context)
                        val candidateSubs = if (streamSubtitles.isNotEmpty()) {
                            streamSubtitles
                        } else {
                            val fetched = try {
                                com.nyyrox.aniverse.data.subtitles.SubtitleRepository.fetchOpenSubtitles(
                                    anilistId = anime.id,
                                    title = anime.title,
                                    episode = episode,
                                    season = 1
                                )
                            } catch (_: Exception) { emptyList() }
                            if (fetched.isNotEmpty()) fetched else {
                                try {
                                    com.nyyrox.aniverse.data.subtitles.SubtitleRepository.searchSubtitlesByTitle(
                                        title = anime.title,
                                        season = 1,
                                        episode = episode
                                    )
                                } catch (_: Exception) { emptyList() }
                            }
                        }

                        if (candidateSubs.isNotEmpty()) {
                            val prioritized = candidateSubs.sortedWith(
                                compareByDescending<com.nyyrox.aniverse.data.subtitles.SubtitleTrack> {
                                    it.language.equals(preferredSubLang, ignoreCase = true) || it.label.contains(preferredSubLang, ignoreCase = true)
                                }.thenByDescending {
                                    it.language.equals("English", ignoreCase = true) || it.label.contains("English", ignoreCase = true)
                                }.thenByDescending { it.isDefault }
                            ).distinctBy { it.language.lowercase(java.util.Locale.US) }.take(6)

                            for (subTrack in prioritized) {
                                try {
                                    val safeLang = subTrack.language.ifBlank { subTrack.label }.replace(Regex("[^a-zA-Z0-9_]"), "_")
                                    val isAss = subTrack.url.contains(".ass", ignoreCase = true) || subTrack.url.contains(".ssa", ignoreCase = true)
                                    val isSrt = subTrack.url.contains(".srt", ignoreCase = true)
                                    val ext = when {
                                        isAss -> ".ass"
                                        isSrt -> ".srt"
                                        else -> ".vtt"
                                    }
                                    val subDestFile = File(epFolder, "sub_${safeLang}$ext")
                                    val dlResult = com.nyyrox.aniverse.data.subtitles.SubtitleRepository.downloadSubtitleFile(context, subTrack)
                                    dlResult.onSuccess { dlFile ->
                                        if (dlFile.exists() && dlFile.length() > 0) {
                                            dlFile.copyTo(subDestFile, overwrite = true)
                                            downloadedSubtitles.add(
                                                subTrack.copy(
                                                    url = subDestFile.absolutePath,
                                                    source = "Downloaded"
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w("DownloadManager", "Failed to download subtitle track ${subTrack.label}: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("DownloadManager", "Error resolving subtitles for download: ${e.message}")
                    }

                    // Save episode-level meta.json
                    val actualEpSize = try {
                        epFolder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    } catch (_: Exception) { 0L }

                    val epMeta = File(epFolder, "meta.json")
                    val epJson = JSONObject().apply {
                        put("animeId", anime.id)
                        put("episode", episode)
                        put("episodeTitle", episodeTitle.orEmpty())
                        put("animeTitle", anime.title)
                        put("animeCover", anime.banner ?: anime.cover)
                        put("providerId", activeProvider.id)
                        put("providerName", activeProvider.name)
                        put("quality", quality)
                        put("audio", audio)
                        put("filePath", finalFile.absolutePath)
                        put("fileSize", if (actualEpSize > 0L) actualEpSize else finalFile.length())
                        put("downloadedAt", System.currentTimeMillis())

                        val subJsonArray = org.json.JSONArray()
                        downloadedSubtitles.forEach { st ->
                            val obj = JSONObject().apply {
                                put("id", st.id)
                                put("label", st.label)
                                put("language", st.language)
                                put("filePath", st.url)
                                put("isDefault", st.isDefault)
                            }
                            subJsonArray.put(obj)
                        }
                        put("subtitles", subJsonArray)
                    }
                    epMeta.writeText(epJson.toString())

                    // Save a real, playable copy where other apps can see it (picked folder or Movies/Zenkai)
                    val exportTree = exportTargets[taskKey] ?: DownloadExporter.defaultTree(context)
                    val wantsOutside = exportTree != null ||
                        DownloadPreferences.settings.value.storageLocation == DownloadStorageLocation.EXTERNAL_PUBLIC
                    if (wantsOutside) {
                        _animeTasks.value = _animeTasks.value + (taskKey to initialTask.copy(
                            providerId = activeProvider.id,
                            providerName = activeProvider.name,
                            progress = 1f,
                            speedFormatted = "Saving to your device...",
                            status = DownloadStatus.DOWNLOADING
                        ))
                        try {
                            val where = DownloadExporter.export(context, finalFile, "${anime.title} - Episode $episode", exportTree)
                            DownloadExporter.toast(context, "Saved to $where")
                        } catch (e: Exception) {
                            Log.e("DownloadManager", "Export failed: ${e.message}", e)
                            DownloadExporter.toast(context, "Downloaded, but saving outside the app failed: ${e.message}")
                        }
                    }
                    exportTargets.remove(taskKey)
                    animeCancellationTokens.remove(taskKey)
                    val completedTask = initialTask.copy(
                        providerId = activeProvider.id,
                        providerName = activeProvider.name,
                        status = DownloadStatus.COMPLETED,
                        progress = 1.0f,
                        localFilePath = finalFile.absolutePath
                    )
                    _animeTasks.value = _animeTasks.value + (taskKey to completedTask)
                    DownloadNotificationHelper.showCompleteNotification(context, completedTask)
                    refreshDownloadedLists()
                    downloadSucceeded = true
                    break
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("DownloadManager", "${activeProvider.name} download attempt $attempt failed: ${e.message}")
                    lastFailureReason = "${activeProvider.name}: ${e.message ?: "Failed"}"
                    if (attempt < maxAttempts) {
                        Log.i("DownloadManager", "Retrying ${activeProvider.name} (attempt ${attempt + 1}/$maxAttempts) after delay...")
                        kotlinx.coroutines.delay(2000L * attempt)
                    }
                }
            }

            if (!downloadSucceeded && !cancelToken.get()) {
                animeCancellationTokens.remove(taskKey)
                val userErrorMessage = "Download stopped on ${activeProvider.name} ($lastFailureReason). Tap Retry to resume."
                val failedTask = initialTask.copy(
                    providerId = activeProvider.id,
                    providerName = activeProvider.name,
                    status = DownloadStatus.FAILED,
                    error = userErrorMessage
                )
                _animeTasks.value = _animeTasks.value + (taskKey to failedTask)
                DownloadNotificationHelper.showFailedNotification(context, failedTask, lastFailureReason)
            }
            }
        }
    }

    // Episode already downloaded in-app: just write the outside copy (no re-download).
    private fun exportExistingEpisode(animeId: Int, episode: Int, title: String, tree: Uri?) {
        scope.launch {
            try {
                val ep = getDownloadedAnimeEpisode(animeId, episode) ?: return@launch
                val where = DownloadExporter.export(context, File(ep.filePath), "$title - Episode $episode", tree)
                DownloadExporter.toast(context, "Saved to $where")
            } catch (e: Exception) {
                Log.e("DownloadManager", "Export failed: ${e.message}", e)
                DownloadExporter.toast(context, "Saving outside the app failed: ${e.message}")
            }
        }
    }

    fun downloadAnimeEpisodes(
        anime: Anime,
        episodes: List<Int>,
        provider: Provider? = null,
        quality: String = "Auto HD",
        audio: String = "Default"
    ) {
        episodes.forEach { ep ->
            downloadAnimeEpisode(
                anime = anime,
                episode = ep,
                provider = provider,
                episodeTitle = "Episode $ep",
                quality = quality,
                audio = audio
            )
        }
    }

    fun retryAnimeDownload(
        anime: Anime,
        episode: Int,
        provider: Provider? = null,
        episodeTitle: String? = null,
        quality: String = "Auto HD",
        audio: String = "Default"
    ) {
        val taskKey = "${anime.id}:$episode"
        val existingTask = _animeTasks.value[taskKey]

        animeCancellationTokens[taskKey]?.set(true)
        animeCancellationTokens.remove(taskKey)
        _animeTasks.value = _animeTasks.value - taskKey

        val targetProvider = provider ?: if (existingTask != null && !existingTask.providerId.isNullOrBlank()) {
            com.nyyrox.aniverse.data.ALL_PROVIDERS.find { it.id == existingTask.providerId }
                ?: com.nyyrox.aniverse.data.extractor.DirectStreamResolver.createProviderForDirectKey(existingTask.providerId, existingTask.audio)
                ?: Provider(
                    id = existingTask.providerId,
                    name = existingTask.providerName.ifBlank { "Direct HD" },
                    pattern = "",
                    serverBadge = "Direct HD",
                    isDirectStream = true
                )
        } else {
            com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(if (audio.contains("DUB", ignoreCase = true)) "dub" else "sub").first()
        }

        downloadAnimeEpisode(
            anime = anime,
            episode = episode,
            provider = targetProvider,
            episodeTitle = episodeTitle ?: existingTask?.episodeTitle,
            quality = if (quality != "Auto HD") quality else (existingTask?.quality ?: "Auto HD"),
            audio = if (audio != "Default") audio else (existingTask?.audio ?: "Default")
        )
    }

    fun deleteDownloadedAnimeEpisode(animeId: Int, episode: Int) {
        scope.launch {
            for (baseDir in allAnimeDirs) {
                val epDir = File(baseDir, "$animeId/$episode")
                if (epDir.exists()) epDir.deleteRecursively()
            }
            refreshDownloadedLists()
        }
    }

    fun deleteDownloadedAnime(animeId: Int) {
        scope.launch {
            for (baseDir in allAnimeDirs) {
                val aDir = File(baseDir, animeId.toString())
                if (aDir.exists()) aDir.deleteRecursively()
            }
            refreshDownloadedLists()
        }
    }

    fun getDownloadedEpisodesForAnime(animeId: Int): List<DownloadedAnimeEpisode> {
        val episodesMap = mutableMapOf<Int, DownloadedAnimeEpisode>()
        for (baseDir in allAnimeDirs) {
            val aDir = File(baseDir, animeId.toString())
            if (!aDir.exists() || !aDir.isDirectory) continue
            val epDirs = aDir.listFiles { f -> f.isDirectory && (File(f, "playlist.m3u8").exists() || File(f, "video.mp4").exists() || File(f, "video.ts").exists()) } ?: continue
            for (epDir in epDirs) {
                val epNum = epDir.name.toIntOrNull() ?: continue
                if (!episodesMap.containsKey(epNum)) {
                    val ep = getDownloadedAnimeEpisode(animeId, epNum)
                    if (ep != null) {
                        episodesMap[epNum] = ep
                    }
                }
            }
        }
        return episodesMap.values.sortedBy { it.episode }
    }

    private fun loadDownloadedAnimeFromDisk(): List<DownloadedAnime> {
        val animeMap = mutableMapOf<Int, DownloadedAnime>()
        for (baseDir in allAnimeDirs) {
            val dirs = baseDir.listFiles() ?: continue
            for (dir in dirs) {
                if (dir.isDirectory) {
                    val animeId = dir.name.toIntOrNull() ?: continue
                    val metaFile = File(dir, "meta.json")
                    var title = "Anime #$animeId"
                    var cover = ""
                    var lastDownloaded = dir.lastModified()

                    if (metaFile.exists()) {
                        try {
                            val mJson = JSONObject(metaFile.readText())
                            title = mJson.optString("title", title)
                            cover = mJson.optString("cover", cover)
                            lastDownloaded = mJson.optLong("lastDownloadedAt", lastDownloaded)
                        } catch (_: Exception) {}
                    }

                    val epDirs = dir.listFiles { f -> f.isDirectory && (File(f, "playlist.m3u8").exists() || File(f, "video.mp4").exists() || File(f, "video.ts").exists()) } ?: emptyArray()
                    val totalSize = try {
                        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    } catch (_: Exception) { 0L }

                    if (epDirs.isNotEmpty()) {
                        val existing = animeMap[animeId]
                        if (existing != null) {
                            animeMap[animeId] = existing.copy(
                                episodesCount = existing.episodesCount + epDirs.size,
                                totalSizeBytes = existing.totalSizeBytes + totalSize,
                                lastDownloadedAt = maxOf(existing.lastDownloadedAt, lastDownloaded),
                                title = if (existing.title.startsWith("Anime #") && !title.startsWith("Anime #")) title else existing.title,
                                cover = existing.cover.ifBlank { cover }
                            )
                        } else {
                            animeMap[animeId] = DownloadedAnime(
                                animeId = animeId,
                                title = title,
                                cover = cover,
                                episodesCount = epDirs.size,
                                totalSizeBytes = totalSize,
                                lastDownloadedAt = lastDownloaded
                            )
                        }
                    }
                }
            }
        }
        return animeMap.values.sortedByDescending { it.lastDownloadedAt }
    }

    // ========================================================================
    // 1. NOVEL DOWNLOADS
    // ========================================================================

    fun isNovelChapterDownloaded(novelSlug: String, chapterSlug: String): Boolean {
        val f = File(baseNovelDir, "$novelSlug/$chapterSlug.json")
        return f.exists() && f.length() > 0
    }

    fun getDownloadedNovelChapter(novelSlug: String, chapterSlug: String): NovelChapterContent? {
        val f = File(baseNovelDir, "$novelSlug/$chapterSlug.json")
        if (!f.exists()) return null
        return try {
            val json = JSONObject(f.readText())
            val paras = mutableListOf<String>()
            val arr = json.optJSONArray("paragraphs")
            if (arr != null) {
                for (i in 0 until arr.length()) paras.add(arr.getString(i))
            }
            NovelChapterContent(
                novelSlug = json.getString("novelSlug"),
                chapterSlug = json.getString("chapterSlug"),
                title = json.getString("title"),
                paragraphs = paras,
                prevChapterSlug = json.optString("prevChapterSlug").takeIf { it.isNotBlank() },
                nextChapterSlug = json.optString("nextChapterSlug").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e("DownloadManager", "Failed to read downloaded novel chapter: ${e.message}")
            null
        }
    }

    fun downloadNovelChapter(
        novelSlug: String,
        novelTitle: String,
        novelCover: String,
        chapter: NovelChapter,
        repo: INovelSourceRepository
    ) {
        val taskKey = "$novelSlug:${chapter.slug}"
        if (isNovelChapterDownloaded(novelSlug, chapter.slug)) return

        val initialTask = NovelDownloadTask(
            novelSlug = novelSlug,
            novelTitle = novelTitle,
            novelCover = novelCover,
            chapterSlug = chapter.slug,
            chapterTitle = chapter.title,
            chapterNumber = chapter.number,
            source = repo.source.id,
            status = DownloadStatus.DOWNLOADING,
            progress = 0.1f
        )
        _novelTasks.value = _novelTasks.value + (taskKey to initialTask)

        scope.launch {
            try {
                val content = repo.getChapterContent(novelSlug, chapter.slug)
                if (content == null || content.paragraphs.isEmpty()) {
                    throw IllegalStateException("Empty chapter content")
                }

                val novelFolder = File(baseNovelDir, novelSlug).apply { if (!exists()) mkdirs() }
                
                // Save meta.json
                val metaFile = File(novelFolder, "meta.json")
                val metaObj = JSONObject().apply {
                    put("title", novelTitle)
                    put("cover", novelCover)
                    put("source", repo.source.id)
                    put("lastDownloadedAt", System.currentTimeMillis())
                }
                metaFile.writeText(metaObj.toString())

                // Save chapter.json
                val chFile = File(novelFolder, "${chapter.slug}.json")
                val chObj = JSONObject().apply {
                    put("novelSlug", content.novelSlug)
                    put("chapterSlug", content.chapterSlug)
                    put("title", content.title)
                    put("prevChapterSlug", content.prevChapterSlug.orEmpty())
                    put("nextChapterSlug", content.nextChapterSlug.orEmpty())
                    val pArray = JSONArray()
                    content.paragraphs.forEach { pArray.put(it) }
                    put("paragraphs", pArray)
                }
                chFile.writeText(chObj.toString())

                _novelTasks.value = _novelTasks.value + (taskKey to initialTask.copy(status = DownloadStatus.COMPLETED, progress = 1.0f))
                refreshDownloadedLists()
            } catch (e: Exception) {
                Log.e("DownloadManager", "Error downloading novel chapter $taskKey: ${e.message}")
                _novelTasks.value = _novelTasks.value + (taskKey to initialTask.copy(status = DownloadStatus.FAILED, error = e.message))
            }
        }
    }

    @JvmName("downloadNovelChaptersModels")
    fun downloadNovelChapters(
        novelSlug: String,
        novelTitle: String,
        novelCover: String,
        chapters: List<NovelChapter>,
        repo: INovelSourceRepository
    ) {
        chapters.forEach { ch ->
            downloadNovelChapter(novelSlug, novelTitle, novelCover, ch, repo)
        }
    }

    fun downloadNovelChapter(
        novelSlug: String,
        novelTitle: String,
        novelCover: String,
        chapterSlug: String,
        chapterTitle: String,
        repo: INovelSourceRepository
    ) {
        downloadNovelChapter(
            novelSlug = novelSlug,
            novelTitle = novelTitle,
            novelCover = novelCover,
            chapter = NovelChapter(slug = chapterSlug, title = chapterTitle),
            repo = repo
        )
    }

    @JvmName("downloadNovelChaptersPairs")
    fun downloadNovelChapters(
        novelSlug: String,
        novelTitle: String,
        novelCover: String,
        chapters: List<Pair<String, String>>,
        repo: INovelSourceRepository
    ) {
        chapters.forEach { (slug, title) ->
            downloadNovelChapter(novelSlug, novelTitle, novelCover, slug, title, repo)
        }
    }

    fun deleteDownloadedNovelChapter(novelSlug: String, chapterSlug: String) {
        scope.launch {
            val f = File(baseNovelDir, "$novelSlug/$chapterSlug.json")
            if (f.exists()) f.delete()
            refreshDownloadedLists()
        }
    }

    fun deleteDownloadedNovel(novelSlug: String) {
        scope.launch {
            val dir = File(baseNovelDir, novelSlug)
            if (dir.exists()) dir.deleteRecursively()
            refreshDownloadedLists()
        }
    }

    private fun loadDownloadedNovelsFromDisk(): List<DownloadedNovel> {
        val list = mutableListOf<DownloadedNovel>()
        val dirs = baseNovelDir.listFiles() ?: return emptyList()
        for (dir in dirs) {
            if (dir.isDirectory) {
                val slug = dir.name
                val metaFile = File(dir, "meta.json")
                var title = slug.replace("-", " ").capitalizeWords()
                var cover = ""
                var source = "novelfire"
                var lastDownloaded = dir.lastModified()

                if (metaFile.exists()) {
                    try {
                        val mJson = JSONObject(metaFile.readText())
                        title = mJson.optString("title", title)
                        cover = mJson.optString("cover", cover)
                        source = mJson.optString("source", source)
                        lastDownloaded = mJson.optLong("lastDownloadedAt", lastDownloaded)
                    } catch (_: Exception) {}
                }

                val chapters = dir.listFiles { _, name -> name.endsWith(".json") && name != "meta.json" } ?: emptyArray()
                val totalSize = dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

                if (chapters.isNotEmpty()) {
                    list.add(
                        DownloadedNovel(
                            novelSlug = slug,
                            title = title,
                            cover = cover,
                            source = source,
                            chaptersCount = chapters.size,
                            totalSizeBytes = totalSize,
                            lastDownloadedAt = lastDownloaded
                        )
                    )
                }
            }
        }
        return list.sortedByDescending { it.lastDownloadedAt }
    }

    fun getDownloadedChaptersForNovel(novelSlug: String): List<DownloadedNovelChapter> {
        val dir = File(baseNovelDir, novelSlug)
        if (!dir.exists()) return emptyList()
        val files = dir.listFiles { _, name -> name.endsWith(".json") && name != "meta.json" } ?: return emptyList()
        return files.mapNotNull { f ->
            try {
                val json = JSONObject(f.readText())
                val chSlug = json.getString("chapterSlug")
                val title = json.getString("title")
                DownloadedNovelChapter(
                    novelSlug = novelSlug,
                    chapterSlug = chSlug,
                    title = title,
                    number = chSlug.substringAfterLast("-", ""),
                    downloadedAt = f.lastModified(),
                    sizeBytes = f.length()
                )
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.chapterSlug }
    }

    // ========================================================================
    // 2. MANGA DOWNLOADS
    // ========================================================================

    fun isMangaChapterDownloaded(mangaId: String, chapterId: String): Boolean {
        val cleanManga = MangaRepository.cleanWeebId(mangaId)
        val cleanCh = MangaRepository.cleanChapterId(chapterId)
        val candidates = listOf(
            File(baseMangaDir, "$cleanManga/$cleanCh"),
            File(baseMangaDir, "$mangaId/$chapterId"),
            File(baseMangaDir, "wc_$cleanManga/wc_chapter_$cleanCh"),
            File(baseMangaDir, "wc_$cleanManga/$cleanCh"),
            File(baseMangaDir, "$cleanManga/wc_chapter_$cleanCh")
        )
        for (dir in candidates) {
            if (dir.exists() && dir.isDirectory) {
                val pages = dir.listFiles { _, name -> name.startsWith("page_") && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".webp")) }
                if (pages != null && pages.isNotEmpty()) return true
            }
        }
        val matchingMangaDir = baseMangaDir.listFiles()?.firstOrNull {
            it.isDirectory && (it.name == mangaId || MangaRepository.cleanWeebId(it.name) == cleanManga)
        }
        val matchingChDir = matchingMangaDir?.listFiles()?.firstOrNull {
            it.isDirectory && (it.name == chapterId || MangaRepository.cleanChapterId(it.name) == cleanCh)
        }
        val pages = matchingChDir?.listFiles { _, name -> name.startsWith("page_") && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".webp")) }
        return pages != null && pages.isNotEmpty()
    }

    fun getDownloadedMangaPages(mangaId: String, chapterId: String): List<String> {
        val cleanManga = MangaRepository.cleanWeebId(mangaId)
        val cleanCh = MangaRepository.cleanChapterId(chapterId)
        val candidates = listOf(
            File(baseMangaDir, "$cleanManga/$cleanCh"),
            File(baseMangaDir, "$mangaId/$chapterId"),
            File(baseMangaDir, "wc_$cleanManga/wc_chapter_$cleanCh"),
            File(baseMangaDir, "wc_$cleanManga/$cleanCh"),
            File(baseMangaDir, "$cleanManga/wc_chapter_$cleanCh")
        )
        val chDir = candidates.firstOrNull { it.exists() && it.isDirectory && (it.listFiles { _, name -> name.startsWith("page_") }?.isNotEmpty() == true) }
            ?: run {
                val matchingMangaDir = baseMangaDir.listFiles()?.firstOrNull {
                    it.isDirectory && (it.name == mangaId || MangaRepository.cleanWeebId(it.name) == cleanManga)
                }
                matchingMangaDir?.listFiles()?.firstOrNull {
                    it.isDirectory && (it.name == chapterId || MangaRepository.cleanChapterId(it.name) == cleanCh)
                }
            } ?: return emptyList()

        val pages = chDir.listFiles { _, name -> name.startsWith("page_") && !name.endsWith(".tmp") } ?: return emptyList()
        if (pages.isEmpty()) return emptyList()
        return pages.sortedBy { f ->
            f.nameWithoutExtension.removePrefix("page_").toIntOrNull() ?: 0
        }.map { it.absolutePath }
    }

    fun downloadMangaChapter(
        mangaId: String,
        mangaTitle: String,
        mangaPoster: String?,
        chapter: MangaChapter
    ) {
        val cleanManga = MangaRepository.cleanWeebId(mangaId)
        val cleanCh = MangaRepository.cleanChapterId(chapter.chapterId)
        val taskKey = "$cleanManga:$cleanCh"
        if (isMangaChapterDownloaded(cleanManga, cleanCh)) return

        val initialTask = MangaDownloadTask(
            mangaId = cleanManga,
            mangaTitle = mangaTitle,
            mangaPoster = mangaPoster,
            chapterId = cleanCh,
            chapterNumber = chapter.number,
            chapterTitle = chapter.title,
            status = DownloadStatus.DOWNLOADING,
            progress = 0.05f
        )
        _mangaTasks.value = _mangaTasks.value + (taskKey to initialTask)

        scope.launch {
            try {
                val pages = MangaRepository.getPages(cleanCh)
                if (pages.isEmpty()) {
                    throw IllegalStateException("No pages found for chapter")
                }

                val mangaFolder = File(baseMangaDir, cleanManga).apply { if (!exists()) mkdirs() }
                
                // Save meta.json for manga
                val metaFile = File(mangaFolder, "meta.json")
                val metaObj = JSONObject().apply {
                    put("title", mangaTitle)
                    put("poster", mangaPoster.orEmpty())
                    put("lastDownloadedAt", System.currentTimeMillis())
                }
                metaFile.writeText(metaObj.toString())

                val chFolder = File(mangaFolder, cleanCh).apply { if (!exists()) mkdirs() }

                var downloadedCount = 0
                for ((index, pageUrl) in pages.withIndex()) {
                    val pageFile = File(chFolder, "page_$index.jpg")
                    if (!pageFile.exists() || pageFile.length() == 0L) {
                        downloadFileWithRetry(pageUrl, pageFile)
                    }
                    downloadedCount++
                    val progress = downloadedCount.toFloat() / pages.size
                    _mangaTasks.value = _mangaTasks.value + (taskKey to initialTask.copy(
                        totalPages = pages.size,
                        downloadedPages = downloadedCount,
                        progress = progress
                    ))
                }

                // Save chapter meta.json
                val chMeta = File(chFolder, "meta.json")
                val chMetaObj = JSONObject().apply {
                    put("chapterNumber", chapter.number)
                    put("chapterTitle", chapter.title.orEmpty())
                    put("pageCount", pages.size)
                    put("downloadedAt", System.currentTimeMillis())
                }
                chMeta.writeText(chMetaObj.toString())

                _mangaTasks.value = _mangaTasks.value + (taskKey to initialTask.copy(
                    totalPages = pages.size,
                    downloadedPages = pages.size,
                    status = DownloadStatus.COMPLETED,
                    progress = 1.0f
                ))
                refreshDownloadedLists()
            } catch (e: Exception) {
                Log.e("DownloadManager", "Error downloading manga chapter $taskKey: ${e.message}")
                _mangaTasks.value = _mangaTasks.value + (taskKey to initialTask.copy(
                    status = DownloadStatus.FAILED,
                    error = e.message
                ))
            }
        }
    }

    @JvmName("downloadMangaChaptersModels")
    fun downloadMangaChapters(
        mangaId: String,
        mangaTitle: String,
        mangaPoster: String?,
        chapters: List<MangaChapter>
    ) {
        chapters.forEach { ch ->
            downloadMangaChapter(mangaId, mangaTitle, mangaPoster, ch)
        }
    }

    fun downloadMangaChapter(
        mangaId: String,
        mangaTitle: String,
        mangaPoster: String?,
        chapterId: String,
        chapterTitle: String
    ) {
        val numMatch = Regex("\\b(?:Chapter|Ch\\.?|Episode|Ep\\.?)\\s*([\\d.]+)").find(chapterTitle)
            ?: Regex("\\b(\\d+(?:\\.\\d+)?)\\b").find(chapterTitle)
        val chNumber = numMatch?.groupValues?.get(1) ?: chapterTitle.removePrefix("Chapter ").trim().ifBlank { "1" }
        downloadMangaChapter(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            mangaPoster = mangaPoster,
            chapter = MangaChapter(chapterId = MangaRepository.cleanChapterId(chapterId), number = chNumber, title = chapterTitle)
        )
    }

    @JvmName("downloadMangaChaptersPairs")
    fun downloadMangaChapters(
        mangaId: String,
        mangaTitle: String,
        mangaPoster: String?,
        chapters: List<Pair<String, String>>
    ) {
        chapters.forEach { (chId, chTitle) ->
            downloadMangaChapter(mangaId, mangaTitle, mangaPoster, chId, chTitle)
        }
    }

    private suspend fun downloadFileWithRetry(url: String, dest: File) = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        val maxAttempts = 3
        
        for (attempt in 1..maxAttempts) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 Aniverse/1.0")
                    .header("Referer", "https://weebcentral.com/")
                    .build()

                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} downloading image")
                    val body = response.body ?: throw IllegalStateException("Empty image body")
                    
                    val tmpDest = File(dest.absolutePath + ".tmp")
                    FileOutputStream(tmpDest).use { fos ->
                        body.byteStream().copyTo(fos)
                    }
                    if (tmpDest.length() > 0) {
                        tmpDest.renameTo(dest)
                        return@withContext
                    } else {
                        tmpDest.delete()
                        throw IllegalStateException("Downloaded file is empty")
                    }
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxAttempts) kotlinx.coroutines.delay(1000L * attempt)
            }
        }
        throw lastError ?: IllegalStateException("Failed to download image after retries")
    }

    fun deleteDownloadedMangaChapter(mangaId: String, chapterId: String) {
        scope.launch {
            val chDir = File(baseMangaDir, "$mangaId/$chapterId")
            if (chDir.exists()) chDir.deleteRecursively()
            refreshDownloadedLists()
        }
    }

    fun deleteDownloadedManga(mangaId: String) {
        scope.launch {
            val dir = File(baseMangaDir, mangaId)
            if (dir.exists()) dir.deleteRecursively()
            refreshDownloadedLists()
        }
    }

    private fun loadDownloadedMangaFromDisk(): List<DownloadedManga> {
        val list = mutableListOf<DownloadedManga>()
        val dirs = baseMangaDir.listFiles() ?: return emptyList()
        for (dir in dirs) {
            if (dir.isDirectory) {
                val mangaId = dir.name
                val metaFile = File(dir, "meta.json")
                var title = mangaId.replace("-", " ").capitalizeWords()
                var poster: String? = null
                var lastDownloaded = dir.lastModified()

                if (metaFile.exists()) {
                    try {
                        val mJson = JSONObject(metaFile.readText())
                        title = mJson.optString("title", title)
                        poster = mJson.optString("poster").takeIf { it.isNotBlank() }
                        lastDownloaded = mJson.optLong("lastDownloadedAt", lastDownloaded)
                    } catch (_: Exception) {}
                }

                val chapters = dir.listFiles { f -> f.isDirectory && File(f, "meta.json").exists() } ?: emptyArray()
                val totalSize = dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

                if (chapters.isNotEmpty()) {
                    list.add(
                        DownloadedManga(
                            mangaId = mangaId,
                            title = title,
                            poster = poster,
                            chaptersCount = chapters.size,
                            totalSizeBytes = totalSize,
                            lastDownloadedAt = lastDownloaded
                        )
                    )
                }
            }
        }
        return list.sortedByDescending { it.lastDownloadedAt }
    }

    fun getDownloadedChaptersForManga(mangaId: String): List<DownloadedMangaChapter> {
        val cleanManga = MangaRepository.cleanWeebId(mangaId)
        val dir = listOf(
            File(baseMangaDir, mangaId),
            File(baseMangaDir, cleanManga),
            File(baseMangaDir, "wc_$cleanManga")
        ).firstOrNull { it.exists() && it.isDirectory } ?: baseMangaDir.listFiles()?.firstOrNull {
            it.isDirectory && (it.name == mangaId || MangaRepository.cleanWeebId(it.name) == cleanManga)
        } ?: return emptyList()

        val chDirs = dir.listFiles { f -> f.isDirectory && (File(f, "meta.json").exists() || f.listFiles { _, name -> name.startsWith("page_") }?.isNotEmpty() == true) } ?: return emptyList()
        return chDirs.mapNotNull { chDir ->
            try {
                val mFile = File(chDir, "meta.json")
                val json = if (mFile.exists()) JSONObject(mFile.readText()) else JSONObject()
                val chNum = json.optString("chapterNumber", chDir.name.removePrefix("wc_chapter_").removePrefix("wc_"))
                val chTitle = json.optString("chapterTitle").takeIf { it.isNotBlank() }
                val pageCount = json.optInt("pageCount", chDir.listFiles { _, n -> n.startsWith("page_") }?.size ?: 0)
                val size = chDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                DownloadedMangaChapter(
                    mangaId = mangaId,
                    chapterId = chDir.name,
                    chapterNumber = chNum,
                    chapterTitle = chTitle,
                    pageCount = pageCount,
                    downloadedAt = chDir.lastModified(),
                    sizeBytes = size
                )
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.chapterNumber.toFloatOrNull() ?: 0f }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
