package com.nyyrox.aniverse.data.novel

enum class NovelSource(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val isLightNovelFocused: Boolean = false
) {
    NOVEL_FULL("novelfull", "NovelFull", "https://novelfull.com", true),
    NOVEL_FIRE("novelfire", "NovelFire", "https://novelfire.net", false),
    WEB_NOVEL("webnovel", "WebNovel", "https://m.webnovel.com", false);

    companion object {
        fun fromId(id: String?): NovelSource {
            if (id.isNullOrBlank()) return NOVEL_FULL
            if (id.equals("novelfire", ignoreCase = true)) return NOVEL_FIRE
            if (id.equals("webnovel", ignoreCase = true)) return WEB_NOVEL
            return NOVEL_FULL
        }
    }
}

data class NovelHomeData(
    val rankings: List<NovelItem> = emptyList(),
    val popular: List<NovelItem> = emptyList(),
    val latest: List<NovelItem> = emptyList(),
    val lightNovels: List<NovelItem> = emptyList()
)

interface INovelSourceRepository {
    val source: NovelSource
    suspend fun getHome(page: Int = 1): NovelHomeData
    suspend fun getRankings(page: Int = 1): List<NovelItem>
    suspend fun getPopular(page: Int = 1): List<NovelItem>
    suspend fun getLatest(page: Int = 1): List<NovelItem>
    suspend fun getLightNovels(page: Int = 1): List<NovelItem>
    suspend fun search(query: String, page: Int = 1): List<NovelItem>
    suspend fun getDetail(slug: String): NovelDetail?
    suspend fun getChapters(slug: String, page: Int = 1): Pair<List<NovelChapter>, Int>
    suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent?
}
