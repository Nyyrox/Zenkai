package com.nyyrox.aniverse.data.manga

interface IMangaSourceRepository {
    val source: MangaSource
    suspend fun getHome(): MangaHome
    suspend fun search(query: String): List<MangaCard>
    suspend fun getInfo(mangaId: String): MangaInfo?
    suspend fun getChapters(mangaId: String): List<MangaChapter>
    suspend fun getPages(chapterId: String): List<String>
}
