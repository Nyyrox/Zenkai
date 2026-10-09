package com.nyyrox.aniverse.data.novel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnifiedNovelRepository(
    val novelFull: NovelFullRepository = NovelFullRepository(),
    val novelFire: NovelFireRepository = NovelFireRepository(),
    val royalRoad: RoyalRoadRepository = RoyalRoadRepository(),
    val wuxiaWorld: WuxiaWorldRepository = WuxiaWorldRepository(),
    val webNovel: WebNovelRepository = WebNovelRepository(royalRoad)
) : INovelSourceRepository {

    private val repositories = mapOf<NovelSource, INovelSourceRepository>(
        NovelSource.NOVEL_FULL to novelFull,
        NovelSource.NOVEL_FIRE to novelFire,
        NovelSource.ROYAL_ROAD to royalRoad,
        NovelSource.WUXIA_WORLD to wuxiaWorld,
        NovelSource.WEB_NOVEL to webNovel
    )

    private val _activeSource = MutableStateFlow(NovelSource.NOVEL_FULL)
    val activeSource: StateFlow<NovelSource> = _activeSource.asStateFlow()

    override val source: NovelSource
        get() = _activeSource.value

    fun setSource(newSource: NovelSource) {
        _activeSource.value = newSource
    }

    fun getRepoForSource(src: NovelSource): INovelSourceRepository {
        return repositories[src] ?: novelFull
    }

    private val currentRepo: INovelSourceRepository
        get() = repositories[_activeSource.value] ?: novelFull

    override suspend fun getHome(page: Int): NovelHomeData = currentRepo.getHome(page)

    override suspend fun getRankings(page: Int): List<NovelItem> = currentRepo.getRankings(page)

    override suspend fun getPopular(page: Int): List<NovelItem> = currentRepo.getPopular(page)

    override suspend fun getLatest(page: Int): List<NovelItem> = currentRepo.getLatest(page)

    override suspend fun getLightNovels(page: Int): List<NovelItem> = currentRepo.getLightNovels(page)

    override suspend fun search(query: String, page: Int): List<NovelItem> {
        val primary = try { currentRepo.search(query, page) } catch (_: Exception) { emptyList() }
        if (primary.isNotEmpty()) return primary

        // Fallback: If active source has no results, search other available sources
        for ((s, repo) in repositories) {
            if (s != _activeSource.value && s != NovelSource.WEB_NOVEL) {
                try {
                    val fallbackResults = repo.search(query, page)
                    if (fallbackResults.isNotEmpty()) {
                        return fallbackResults
                    }
                } catch (_: Exception) {}
            }
        }
        return emptyList()
    }

    override suspend fun getDetail(slug: String): NovelDetail? {
        try {
            val res = currentRepo.getDetail(slug)
            if (res != null) return res
        } catch (_: Exception) {}
        for ((s, repo) in repositories) {
            if (s != _activeSource.value) {
                try {
                    val res = repo.getDetail(slug)
                    if (res != null) return res
                } catch (_: Exception) {}
            }
        }
        // Fallback: If slug is a raw title, search across sources and resolve best match
        try {
            val queryCandidate = slug.trim()
            if (queryCandidate.length >= 2) {
                val searchResults = search(queryCandidate, 1)
                if (searchResults.isNotEmpty()) {
                    val matched = searchResults.first()
                    for ((_, repo) in repositories) {
                        try {
                            val res = repo.getDetail(matched.slug)
                            if (res != null) return res
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    override suspend fun getChapters(slug: String, page: Int): Pair<List<NovelChapter>, Int> {
        try {
            val (chs, maxP) = currentRepo.getChapters(slug, page)
            if (chs.isNotEmpty()) return Pair(chs, maxP)
        } catch (_: Exception) {}
        for ((s, repo) in repositories) {
            if (s != _activeSource.value) {
                try {
                    val (chs, maxP) = repo.getChapters(slug, page)
                    if (chs.isNotEmpty()) return Pair(chs, maxP)
                } catch (_: Exception) {}
            }
        }
        return Pair(emptyList(), 1)
    }

    override suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent? {
        try {
            val content = currentRepo.getChapterContent(novelSlug, chapterSlug)
            if (content != null) return content
        } catch (_: Exception) {}
        for ((s, repo) in repositories) {
            if (s != _activeSource.value) {
                try {
                    val content = repo.getChapterContent(novelSlug, chapterSlug)
                    if (content != null) return content
                } catch (_: Exception) {}
            }
        }
        return null
    }
}
