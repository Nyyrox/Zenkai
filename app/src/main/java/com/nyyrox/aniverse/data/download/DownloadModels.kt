package com.nyyrox.aniverse.data.download

enum class DownloadType {
    ANIME,
    MOVIE,
    NOVEL,
    MANGA
}

enum class DownloadStatus {
    IDLE,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class AnimeDownloadTask(
    val animeId: Int,
    val animeTitle: String,
    val animeCover: String,
    val episode: Int,
    val episodeTitle: String? = null,
    val providerId: String,
    val providerName: String,
    val quality: String = "Auto HD",
    val audio: String = "SUB",
    val totalSegments: Int = 0,
    val downloadedSegments: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedFormatted: String = "",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val error: String? = null,
    val localFilePath: String? = null
)

data class MovieDownloadTask(
    val movieId: Int,
    val title: String,
    val poster: String? = null,
    val backdrop: String? = null,
    val type: String = "movie", // "movie" or "tv"
    val season: Int = 1,
    val episode: Int = 1,
    val episodeTitle: String? = null,
    val quality: String = "Auto HD",
    val dubId: String? = null,
    val totalSegments: Int = 0,
    val downloadedSegments: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedFormatted: String = "",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val error: String? = null,
    val localFilePath: String? = null
)

data class NovelDownloadTask(
    val novelSlug: String,
    val novelTitle: String,
    val novelCover: String,
    val chapterSlug: String,
    val chapterTitle: String,
    val chapterNumber: String? = null,
    val source: String = "novelfire",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val error: String? = null
)

data class MangaDownloadTask(
    val mangaId: String,
    val mangaTitle: String,
    val mangaPoster: String? = null,
    val chapterId: String,
    val chapterNumber: String,
    val chapterTitle: String? = null,
    val totalPages: Int = 0,
    val downloadedPages: Int = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val error: String? = null
)

data class DownloadedAnime(
    val animeId: Int,
    val title: String,
    val cover: String,
    val banner: String? = null,
    val description: String? = null,
    val score: Double? = null,
    val format: String? = null,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val season: String? = null,
    val year: Int? = null,
    val episodesCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val lastDownloadedAt: Long = System.currentTimeMillis()
)

data class DownloadedAnimeEpisode(
    val animeId: Int,
    val episode: Int,
    val episodeTitle: String? = null,
    val animeTitle: String = "",
    val animeCover: String = "",
    val episodeStill: String? = null,
    val episodeOverview: String? = null,
    val providerId: String = "",
    val providerName: String = "",
    val filePath: String,
    val localUri: String = filePath,
    val sizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val subtitles: List<com.nyyrox.aniverse.data.subtitles.SubtitleTrack> = emptyList()
)

data class DownloadedMovie(
    val movieId: Int,
    val title: String,
    val poster: String? = null,
    val backdrop: String? = null,
    val overview: String? = null,
    val year: String? = null,
    val type: String = "movie", // "movie" or "tv"
    val episodesCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val lastDownloadedAt: Long = System.currentTimeMillis()
)

data class DownloadedMovieEpisode(
    val movieId: Int,
    val season: Int = 1,
    val episode: Int = 1,
    val episodeTitle: String? = null,
    val movieTitle: String = "",
    val poster: String? = null,
    val backdrop: String? = null,
    val overview: String? = null,
    val filePath: String,
    val localUri: String = filePath,
    val sizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val quality: String = "Auto HD"
)

data class DownloadedNovel(
    val novelSlug: String,
    val title: String,
    val cover: String,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val source: String = "novelfire",
    val chaptersCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val lastDownloadedAt: Long = System.currentTimeMillis()
)

data class DownloadedNovelChapter(
    val novelSlug: String,
    val chapterSlug: String,
    val title: String,
    val number: String? = null,
    val downloadedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L
)

data class DownloadedManga(
    val mangaId: String,
    val title: String,
    val poster: String? = null,
    val description: String? = null,
    val banner: String? = null,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val chaptersCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val lastDownloadedAt: Long = System.currentTimeMillis()
)

data class DownloadedMangaChapter(
    val mangaId: String,
    val chapterId: String,
    val chapterNumber: String,
    val chapterTitle: String? = null,
    val pageCount: Int = 0,
    val downloadedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L
)

data class DownloadTaskInfo(
    val type: DownloadType,
    val itemId: String,
    val chapterId: String,
    val title: String,
    val itemTitle: String = "",
    val cover: String? = null,
    val status: DownloadStatus,
    val progress: Float,
    val downloadedCount: Int = 0,
    val totalCount: Int = 0,
    val speed: String = "",
    val error: String? = null,
    val providerId: String? = null,
    val providerName: String? = null,
    val audio: String? = null,
    val quality: String? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L
) {
    val percentage: Int
        get() {
            val p = if (progress.isNaN() || !progress.isFinite()) 0f else progress.coerceIn(0f, 1f)
            return (p * 100).toInt()
        }
}
