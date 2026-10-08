package com.nyyrox.aniverse.data.manga
 
data class MangaCard(
    val id: String,
    val title: String,
    val poster: String? = null,
    val type: String? = "Manga",
    val chapter: String? = null,
    val updated: String? = null,
    val source: String = "weebcentral"
)

data class MangaInfo(
    val id: String,
    val title: String,
    val altTitle: String? = null,
    val poster: String? = null,
    val description: String = "",
    val type: String? = "Manga",
    val status: String? = null,
    val rating: String? = null,
    val genres: List<String> = emptyList(),
    val source: String = "weebcentral",
    val banner: String? = null,
    val anilistId: Int? = null,
    val totalChapters: Int? = null,
    val authors: List<String> = emptyList()
)

data class MangaChapter(
    val number: String,
    val title: String? = null,
    val chapterId: String,
    val releaseDate: String? = null,
    val url: String = "",
    val group: String? = null,
    val volume: String? = null,
    val source: String = "weebcentral"
)

data class MangaHome(
    val trending: List<MangaCard> = emptyList(),
    val latest: List<MangaCard> = emptyList(),
    val newRelease: List<MangaCard> = emptyList()
)

enum class MangaBookmarkType {
    SERIES,
    CHAPTER,
    PAGE
}

data class MangaBookmarkItem(
    val mangaId: String,
    val title: String,
    val poster: String? = null,
    val type: String? = "Manga",
    val bookmarkType: MangaBookmarkType = MangaBookmarkType.SERIES,
    val chapterId: String? = null,
    val chapterNumber: String? = null,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val latestChapter: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class MangaHistoryItem(
    val mangaId: String,
    val title: String,
    val poster: String? = null,
    val type: String? = "Manga",
    val chapterId: String,
    val chapterNumber: String,
    val chapterTitle: String? = null,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class MangaReaderMode {
    VERTICAL_WEBTOON,
    HORIZONTAL_RTL,
    HORIZONTAL_LTR,
    HORIZONTAL_PAGED;

    val isPaged: Boolean get() = this == HORIZONTAL_RTL || this == HORIZONTAL_LTR || this == HORIZONTAL_PAGED
    val isRtl: Boolean get() = this == HORIZONTAL_RTL || this == HORIZONTAL_PAGED
}

