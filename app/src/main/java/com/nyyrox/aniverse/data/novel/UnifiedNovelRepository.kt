package com.nyyrox.aniverse.data.novel

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

class UnifiedNovelRepository(
    val lnori: LnoriRepository = LnoriRepository(),
    val novelFull: NovelFullRepository = NovelFullRepository(),
    val novelFire: NovelFireRepository = NovelFireRepository(),
    val freeWebNovel: FreeWebNovelRepository = FreeWebNovelRepository(),
    val royalRoad: RoyalRoadRepository = RoyalRoadRepository(),
    val wuxiaWorld: WuxiaWorldRepository = WuxiaWorldRepository(),
    val webNovel: WebNovelRepository = WebNovelRepository(royalRoad)
) : INovelSourceRepository {

    private val repositories = mapOf<NovelSource, INovelSourceRepository>(
        NovelSource.LNORI to lnori,
        NovelSource.NOVEL_FULL to novelFull,
        NovelSource.NOVEL_FIRE to novelFire,
        NovelSource.FREE_WEB_NOVEL to freeWebNovel,
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

    override suspend fun search(query: String, page: Int): List<NovelItem> = coroutineScope {
        val candidates = mutableListOf(query.trim())
        if (query.contains("class room", ignoreCase = true)) {
            candidates.add(query.replace("class room", "classroom", ignoreCase = true))
        }
        if (query.contains("alya", ignoreCase = true)) {
            candidates.add("alya")
            candidates.add("roshidere")
        }
        if (query.contains("bunny girl", ignoreCase = true)) {
            candidates.add("bunny girl")
            candidates.add("rascal does not dream")
        }
        val words = query.trim().split("\\s+".toRegex())
        if (words.size > 3) {
            candidates.add(words.take(3).joinToString(" "))
            candidates.add(words.take(2).joinToString(" "))
        }

        // Search across ALL sources in parallel: Lnori, NovelFull, NovelFire, FreeWebNovel, RoyalRoad, WuxiaWorld
        val targetRepos = listOf(lnori, novelFull, novelFire, freeWebNovel, royalRoad, wuxiaWorld)
        val deferredList = targetRepos.map { repo ->
            async {
                val resultsForRepo = mutableListOf<NovelItem>()
                for (q in candidates.distinct()) {
                    try {
                        withTimeoutOrNull(5000L) {
                            val res = repo.search(q, page)
                            for (item in res) {
                                if (resultsForRepo.none { it.slug == item.slug }) {
                                    resultsForRepo.add(item)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                    if (resultsForRepo.isNotEmpty()) break
                }
                resultsForRepo
            }
        }

        val allResults = mutableListOf<NovelItem>()
        val seenSlugs = mutableSetOf<String>()
        val seenKeys = mutableSetOf<String>()

        val searchOutputs = deferredList.awaitAll()
        for (repoResults in searchOutputs) {
            for (item in repoResults) {
                val normTitle = item.title.lowercase().replace("[^a-z0-9]".toRegex(), "")
                val key = "$normTitle:${item.source}"
                if (item.slug !in seenSlugs && key !in seenKeys) {
                    seenSlugs.add(item.slug)
                    seenKeys.add(key)
                    allResults.add(item)
                }
            }
        }

        allResults
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
