package com.nyyrox.aniverse.data.animesalt

import com.nyyrox.aniverse.data.mapping.AnimeMappingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AnimeSaltEpisode(
    val season: Int,
    val episode: Int,
    val absoluteEpisode: Int,
    val title: String? = null,
    val url: String,
    val slug: String
)

data class ResolvedEpisode(
    val anilistEpisode: Int,
    val tmdbEpisode: Int? = null,
    val animeSaltSeason: Int,
    val animeSaltEpisode: Int,
    val animeSaltUrl: String,
    val slug: String
) {
    // Backward-compat property alias
    val episodeUrl: String get() = animeSaltUrl
}

typealias EpisodeMapping = ResolvedEpisode

data class AnimeSaltSeason(
    val number: Int,
    val episodeCount: Int,
    val episodes: List<Int> = emptyList()
)

data class AnimeSaltSeriesRelationship(
    val anilistId: Int,
    val matchedTitle: String,
    val normalizedTitle: String,
    val seriesSlug: String,
    val indexedEpisodes: List<AnimeSaltEpisode> = emptyList(),
    val episodeMap: Map<Int, ResolvedEpisode>
) {
    val seasons: List<AnimeSaltSeason>
        get() {
            val bySeason = indexedEpisodes.groupBy { it.season }
            return bySeason.map { (sNum, eps) ->
                AnimeSaltSeason(number = sNum, episodeCount = eps.size, episodes = eps.map { it.episode })
            }.sortedBy { it.number }
        }
}

object AnimeSaltResolver {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val BASE_URL = "https://animesalt.cx"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    // Cache of series relationships by anilistId
    private val seriesCache = ConcurrentHashMap<Int, AnimeSaltSeriesRelationship>()

    /**
     * Canonical AnimeSalt Title Normalization:
     * 1. lowercase
     * 2. remove punctuation/symbols: [^\p{L}\p{N}\s-] (punctuation does NOT turn into hyphen)
     * 3. whitespace → "-"
     * 4. collapse repeated "-"
     * 5. trim "-"
     *
     * Examples:
     * - Attack on Titan -> attack-on-titan
     * - Solo Leveling -> solo-leveling
     * - DAN DA DAN -> dan-da-dan
     * - Kaiju No. 8 -> kaiju-no-8
     * - Bleach: Thousand-Year Blood War -> bleach-thousand-year-blood-war
     * - Robotics;Notes -> roboticsnotes
     * - Steins;Gate -> steinsgate
     */
    fun normalizeAnimeSaltTitle(title: String): String {
        return title
            .lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s-]"""), "")
            .replace(Regex("""\s+"""), "-")
            .replace(Regex("""-+"""), "-")
            .trim('-')
    }

    /**
     * Exact episode slug generator:
     * {normalizedTitle}-{season}x{episode}
     */
    fun generateAnimeSaltSlug(
        title: String,
        season: Int,
        episode: Int
    ): String {
        val normalized = normalizeAnimeSaltTitle(title)
        return "$normalized-${season}x$episode"
    }

    /**
     * Backward-compatible aliases for normalizeAnimeSaltTitle
     */
    fun makeProviderSlug(value: String): String = normalizeAnimeSaltTitle(value)
    fun normalizeTitle(title: String): String = normalizeAnimeSaltTitle(title)

    /**
     * Extracts season number from title or romaji string (e.g. "Season 2", "2nd Season", "Part 2", "S2")
     */
    fun extractSeason(title: String, romajiTitle: String? = null): Int {
        val patterns = listOf(
            Regex("\\b(?:Season|Series|Part|S)\\s*(\\d+)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(\\d+)(?:st|nd|rd|th)\\s+Season\\b", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(title)
            if (m != null) return m.groupValues[1].toIntOrNull() ?: 1
        }
        if (!romajiTitle.isNullOrBlank()) {
            for (p in patterns) {
                val m = p.find(romajiTitle)
                if (m != null) return m.groupValues[1].toIntOrNull() ?: 1
            }
        }
        return 1
    }

    /**
     * Derives base series slug stripping trailing year, season tags, and subtitles.
     */
    fun tmdbAccurateSlugBase(tmdbName: String): String {
        var name = tmdbName.trim()
        name = name.replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "").replace(Regex("\\s+\\d{4}\\s*$"), "").trim()
        name = name.replace(Regex("\\s*([–—:]?\\s*)(?:Season|Series|Part|S)\\s*\\d+.*$", RegexOption.IGNORE_CASE), "").trim()
        name = name.replace(Regex("\\s*:\\s*(?:the\\s+)?(?:final|last)\\s+(?:season|arc).*$", RegexOption.IGNORE_CASE), "").trim()
        val colonPart = name.substringBefore(':').trim()
        val slug = normalizeAnimeSaltTitle(name)
        return slug.ifBlank { normalizeAnimeSaltTitle(colonPart) }
    }

    /**
     * Generates episode slug: ${baseSlug}-${season}x${episode} or ${baseSlug} for movies
     */
    fun animeSaltEpisodeSlug(baseSlug: String, season: Int, episode: Int, isMovie: Boolean): String {
        val clean = normalizeAnimeSaltTitle(baseSlug.removePrefix("salt-").replace(Regex("-\\d+x\\d+$", RegexOption.IGNORE_CASE), ""))
        if (clean.isBlank()) return ""
        return if (isMovie) clean else generateAnimeSaltSlug(clean, maxOf(1, season), maxOf(1, episode))
    }

    /**
     * Generates candidate episode slugs in priority order.
     * Uses anime-mapping.json direct mapping if available, else candidate titles and rules.
     */
    fun getCandidateSlugs(
        title: String,
        romajiTitle: String? = null,
        format: String? = null,
        episode: Int = 1,
        anilistId: Int = 0
    ): List<String> {
        val isMovie = format?.equals("MOVIE", ignoreCase = true) == true
        val season = extractSeason(title, romajiTitle)
        val candidates = mutableListOf<String>()

        // 0. Direct mapping from anime-mapping.json if available
        if (anilistId > 0 && AnimeMappingRepository.isMapped(anilistId)) {
            val candidate = AnimeMappingRepository.resolveCandidate(anilistId, episode, title)
            val s = candidate.season ?: season
            val finalEp = candidate.finalEp
            val directSlug = if (candidate.isMovie || isMovie) candidate.saltId else generateAnimeSaltSlug(candidate.saltId, s, finalEp)
            candidates.add(directSlug)
            for (cs in candidate.candidateSlugs) {
                candidates.add(if (candidate.isMovie || isMovie) cs else generateAnimeSaltSlug(cs, s, finalEp))
            }
        }

        // 1. Primary title normalized
        val normTitle = normalizeAnimeSaltTitle(title)
        if (normTitle.isNotBlank()) {
            candidates.add(animeSaltEpisodeSlug(normTitle, season, episode, isMovie))
            if (season > 1) {
                candidates.add(animeSaltEpisodeSlug(normTitle, 1, episode, isMovie))
            }
        }

        // 2. Stripped base title
        val base1 = tmdbAccurateSlugBase(title)
        if (base1.isNotBlank() && base1 != normTitle) {
            candidates.add(animeSaltEpisodeSlug(base1, season, episode, isMovie))
            if (season > 1) {
                candidates.add(animeSaltEpisodeSlug(base1, 1, episode, isMovie))
            }
        }

        // 3. Romaji title
        if (!romajiTitle.isNullOrBlank()) {
            val base2 = tmdbAccurateSlugBase(romajiTitle)
            if (base2.isNotBlank() && base2 != base1 && base2 != normTitle) {
                candidates.add(animeSaltEpisodeSlug(base2, season, episode, isMovie))
                if (season > 1) {
                    candidates.add(animeSaltEpisodeSlug(base2, 1, episode, isMovie))
                }
            }
        }

        return candidates.distinct()
    }

    /**
     * Resolves and builds the complete AnimeSalt relationship once for an anime:
     * 1. Check anime-mapping.json (AnimeMappingRepository) for direct slug/season mapping.
     * 2. If absent: AniList Title -> TMDB Mapped Title -> AnimeSalt Series Match -> Read ALL Seasons/Episodes -> Deterministic episodeMap.
     * AniList remains the episode-order authority.
     */
    suspend fun getOrBuildRelationship(
        anilistId: Int,
        title: String,
        romajiTitle: String? = null,
        tmdbTitle: String? = null,
        format: String? = null,
        totalEpisodes: Int = 1
    ): AnimeSaltSeriesRelationship? = withContext(Dispatchers.IO) {
        seriesCache[anilistId]?.let { return@withContext it }

        try {
            if (anilistId > 0 && !AnimeMappingRepository.isMapped(anilistId)) {
                // Unmapped in anime-mapping.json -> Not available on AnimeSalt / ACDN
                return@withContext null
            }

            val isMovie = format?.equals("MOVIE", ignoreCase = true) == true
            val targetSeason = extractSeason(title, romajiTitle)

            // ── TIER 1: Direct Mapping via anime-mapping.json (AnimeMappingRepository) ──
            val directEntries = AnimeMappingRepository.getEntries(anilistId)
            if (directEntries.isNotEmpty()) {
                val firstEntry = directEntries.first()
                val mappedSaltId = firstEntry.saltId
                val movieFlag = isMovie || firstEntry.season == null || firstEntry.season == 0

                if (movieFlag) {
                    val relationship = AnimeSaltSeriesRelationship(
                        anilistId = anilistId,
                        matchedTitle = title,
                        normalizedTitle = mappedSaltId,
                        seriesSlug = mappedSaltId,
                        indexedEpisodes = listOf(
                            AnimeSaltEpisode(
                                season = 1,
                                episode = 1,
                                absoluteEpisode = 1,
                                title = title,
                                url = "$BASE_URL/movies/$mappedSaltId/",
                                slug = mappedSaltId
                            )
                        ),
                        episodeMap = mapOf(
                            1 to ResolvedEpisode(
                                anilistEpisode = 1,
                                tmdbEpisode = 1,
                                animeSaltSeason = 1,
                                animeSaltEpisode = 1,
                                animeSaltUrl = "$BASE_URL/movies/$mappedSaltId/",
                                slug = mappedSaltId
                            )
                        )
                    )
                    seriesCache[anilistId] = relationship
                    return@withContext relationship
                }

                // Concurrently index provider episodes from series page if reachable
                val indexedEpisodes = fetchAllSeriesEpisodes(mappedSaltId, mappedSaltId, totalEpisodes)

                val episodeMap = mutableMapOf<Int, ResolvedEpisode>()
                for (ep in 1..maxOf(totalEpisodes, 1)) {
                    val candidate = AnimeMappingRepository.resolveCandidate(anilistId, ep, title)
                    val s = candidate.season ?: targetSeason
                    val finalEp = candidate.finalEp

                    val matchedIndexed = indexedEpisodes.find { it.season == s && it.episode == finalEp }
                        ?: (if (s <= 1) indexedEpisodes.find { it.absoluteEpisode == ep } else null)

                    val slug = when {
                        candidate.isMovie -> candidate.saltId
                        matchedIndexed != null -> matchedIndexed.slug
                        else -> generateAnimeSaltSlug(candidate.saltId, s, finalEp)
                    }
                    val seasonRes = matchedIndexed?.season ?: s
                    val epRes = matchedIndexed?.episode ?: finalEp

                    episodeMap[ep] = ResolvedEpisode(
                        anilistEpisode = ep,
                        tmdbEpisode = epRes,
                        animeSaltSeason = seasonRes,
                        animeSaltEpisode = epRes,
                        animeSaltUrl = if (candidate.isMovie) "$BASE_URL/movies/$slug/" else "$BASE_URL/episode/$slug/",
                        slug = slug
                    )
                }

                val relationship = AnimeSaltSeriesRelationship(
                    anilistId = anilistId,
                    matchedTitle = title,
                    normalizedTitle = mappedSaltId,
                    seriesSlug = mappedSaltId,
                    indexedEpisodes = indexedEpisodes,
                    episodeMap = episodeMap
                )
                seriesCache[anilistId] = relationship
                return@withContext relationship
            }

            // ── TIER 2: Dynamic Mapping via TMDB Title & AnimeSalt Catalog ──
            val resolvedTmdbTitle = tmdbTitle?.takeIf { it.isNotBlank() } ?: run {
                try {
                    com.nyyrox.aniverse.data.tmdb.TmdbRepository.getDetails(anilistId, title, null, format)?.title
                        ?: com.nyyrox.aniverse.data.mapping.AniBridgeResolver.resolve(anilistId)?.title
                } catch (_: Exception) { null }
            }

            val candidateTitles = listOfNotNull(
                resolvedTmdbTitle?.takeIf { it.isNotBlank() },
                title.takeIf { it.isNotBlank() },
                romajiTitle?.takeIf { it.isNotBlank() }
            ).distinct()

            var matchedSeriesSlug: String? = null
            var matchedTitle: String = candidateTitles.firstOrNull() ?: title

            if (isMovie) {
                // Movie-specific candidate probing & search:
                for (cand in candidateTitles) {
                    val norm = normalizeAnimeSaltTitle(cand)
                    if (norm.isNotBlank()) {
                        val candidates = listOf(norm, "$norm-the-movie", "$norm-movie")
                        for (c in candidates) {
                            if (probeMovieExists(c)) {
                                matchedSeriesSlug = c
                                matchedTitle = cand
                                break
                            }
                        }
                        if (matchedSeriesSlug != null) break
                    }
                    val stripped = tmdbAccurateSlugBase(cand)
                    if (stripped.isNotBlank() && stripped != norm) {
                        val candidates = listOf(stripped, "$stripped-the-movie", "$stripped-movie")
                        for (c in candidates) {
                            if (probeMovieExists(c)) {
                                matchedSeriesSlug = c
                                matchedTitle = cand
                                break
                            }
                        }
                        if (matchedSeriesSlug != null) break
                    }
                }

                if (matchedSeriesSlug == null) {
                    for (cand in candidateTitles) {
                        val searchSlug = searchAnimeSalt(cand, isMovie = true)
                        if (searchSlug != null) {
                            matchedSeriesSlug = searchSlug
                            matchedTitle = cand
                            break
                        }
                    }
                }

                if (matchedSeriesSlug == null) {
                    matchedSeriesSlug = normalizeAnimeSaltTitle(matchedTitle)
                }

                val movieSlug = matchedSeriesSlug
                val relationship = AnimeSaltSeriesRelationship(
                    anilistId = anilistId,
                    matchedTitle = matchedTitle,
                    normalizedTitle = movieSlug,
                    seriesSlug = movieSlug,
                    indexedEpisodes = listOf(
                        AnimeSaltEpisode(
                            season = 1,
                            episode = 1,
                            absoluteEpisode = 1,
                            title = matchedTitle,
                            url = "$BASE_URL/movies/$movieSlug/",
                            slug = movieSlug
                        )
                    ),
                    episodeMap = mapOf(
                        1 to ResolvedEpisode(
                            anilistEpisode = 1,
                            tmdbEpisode = 1,
                            animeSaltSeason = 1,
                            animeSaltEpisode = 1,
                            animeSaltUrl = "$BASE_URL/movies/$movieSlug/",
                            slug = movieSlug
                        )
                    )
                )
                seriesCache[anilistId] = relationship
                return@withContext relationship
            }

            // 1. Direct series URL probe using normalized titles
            for (cand in candidateTitles) {
                val norm = normalizeAnimeSaltTitle(cand)
                if (norm.isNotBlank() && probeSeriesExists(norm)) {
                    matchedSeriesSlug = norm
                    matchedTitle = cand
                    break
                }
                val stripped = tmdbAccurateSlugBase(cand)
                if (stripped.isNotBlank() && stripped != norm && probeSeriesExists(stripped)) {
                    matchedSeriesSlug = stripped
                    matchedTitle = cand
                    break
                }
            }

            // 2. If direct probe misses, search AnimeSalt series catalog
            if (matchedSeriesSlug == null) {
                for (cand in candidateTitles) {
                    val searchSlug = searchAnimeSalt(cand)
                    if (searchSlug != null) {
                        matchedSeriesSlug = searchSlug
                        matchedTitle = cand
                        break
                    }
                }
            }

            if (matchedSeriesSlug == null) {
                matchedSeriesSlug = tmdbAccurateSlugBase(matchedTitle).ifBlank { normalizeAnimeSaltTitle(matchedTitle) }
            }

            val normalizedTitle = matchedSeriesSlug

            // 3. Read ALL AnimeSalt seasons and episodes into indexed list
            val indexedEpisodes = fetchAllSeriesEpisodes(matchedSeriesSlug, normalizedTitle, totalEpisodes)

            // 4. Build deterministic episodeMap matching AniList episode -> ResolvedEpisode
            val episodeMap = buildDeterministicEpisodeMap(
                normalizedTitle = normalizedTitle,
                indexedEpisodes = indexedEpisodes,
                targetSeason = targetSeason,
                totalEpisodes = totalEpisodes,
                isMovie = isMovie
            )

            val relationship = AnimeSaltSeriesRelationship(
                anilistId = anilistId,
                matchedTitle = matchedTitle,
                normalizedTitle = normalizedTitle,
                seriesSlug = matchedSeriesSlug,
                indexedEpisodes = indexedEpisodes,
                episodeMap = episodeMap
            )

            seriesCache[anilistId] = relationship
            relationship
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Lookup episode in pre-computed episodeMap, direct anime-mapping.json, or compute deterministic fallback.
     */
    fun resolveAnimeSaltEpisode(
        relationship: AnimeSaltSeriesRelationship?,
        title: String,
        romajiTitle: String? = null,
        format: String? = null,
        anilistEpisode: Int,
        anilistId: Int = 0
    ): ResolvedEpisode {
        relationship?.episodeMap?.get(anilistEpisode)?.let { return it }

        val isMovie = format?.equals("MOVIE", ignoreCase = true) == true
        if (anilistId > 0 && AnimeMappingRepository.isMapped(anilistId)) {
            val candidate = AnimeMappingRepository.resolveCandidate(anilistId, anilistEpisode, title)
            val s = candidate.season ?: 1
            val finalEp = candidate.finalEp
            val slug = if (candidate.isMovie || isMovie) candidate.saltId else generateAnimeSaltSlug(candidate.saltId, s, finalEp)
            return ResolvedEpisode(
                anilistEpisode = anilistEpisode,
                tmdbEpisode = finalEp,
                animeSaltSeason = s,
                animeSaltEpisode = finalEp,
                animeSaltUrl = if (candidate.isMovie || isMovie) "$BASE_URL/movies/$slug/" else "$BASE_URL/episode/$slug/",
                slug = slug
            )
        }

        val season = extractSeason(title, romajiTitle)
        val baseSlug = relationship?.normalizedTitle ?: tmdbAccurateSlugBase(title)

        val slug = animeSaltEpisodeSlug(baseSlug, season, anilistEpisode, isMovie)
        return ResolvedEpisode(
            anilistEpisode = anilistEpisode,
            tmdbEpisode = anilistEpisode,
            animeSaltSeason = season,
            animeSaltEpisode = anilistEpisode,
            animeSaltUrl = if (isMovie) "$BASE_URL/movies/$slug/" else "$BASE_URL/episode/$slug/",
            slug = slug
        )
    }

    data class AnimeSaltServerEntry(
        val name: String,
        val url: String
    )

    data class AnimeSaltExtractedServers(
        val myStreamUrl: String?,
        val abyssUrl: String?,
        val allServers: List<AnimeSaltServerEntry> = emptyList()
    )

    private val serversCache = ConcurrentHashMap<String, AnimeSaltExtractedServers>()

    private fun stripTags(html: String): String {
        return html.replace(Regex("<[^>]+>"), "").trim()
    }

    fun parseServers(html: String): List<AnimeSaltServerEntry> {
        val serverNames = mutableListOf<String>()

        // ── A. Collect server display names from .server-btn blocks ──
        val btnBlockPattern = Pattern.compile("class=[\"'][^\"']*server-btn[^\"']*[\"'][\\s\\S]*?(?=class=[\"'][^\"']*server-btn|$)", Pattern.CASE_INSENSITIVE)
        val btnMatcher = btnBlockPattern.matcher(html)
        while (btnMatcher.find()) {
            val block = btnMatcher.group(0) ?: ""
            val infoM = Pattern.compile("class=[\"'][^\"']*server-info[^\"']*[\"'][^>]*>([\\s\\S]*?)<\\/[a-zA-Z]+>", Pattern.CASE_INSENSITIVE).matcher(block)
            val nameM = Pattern.compile("class=[\"'][^\"']*server-name[^\"']*[\"'][^>]*>([\\s\\S]*?)<\\/[a-zA-Z]+>", Pattern.CASE_INSENSITIVE).matcher(block)
            val label = when {
                infoM.find() -> stripTags(infoM.group(1).orEmpty())
                nameM.find() -> stripTags(nameM.group(1).orEmpty())
                else -> ""
            }
            serverNames.add(label.ifBlank { "Server ${serverNames.size + 1}" })
        }

        if (serverNames.isEmpty()) {
            val infoFallback = Pattern.compile("class=[\"'][^\"']*server-info[^\"']*[\"'][^>]*>([\\s\\S]*?)<\\/[a-zA-Z]+>", Pattern.CASE_INSENSITIVE).matcher(html)
            while (infoFallback.find()) {
                val label = stripTags(infoFallback.group(1).orEmpty())
                serverNames.add(label.ifBlank { "Server ${serverNames.size + 1}" })
            }
        }

        // ── B. iframe URLs inside .video.aa-tb container ──
        val vcPattern = Pattern.compile("class=[\"'][^\"']*\\bvideo\\b[^\"']*\\baa-tb\\b[^\"']*[\"'][\\s\\S]*?(?=<\\/div>\\s*<\\/div>|$)|class=[\"'][^\"']*\\baa-tb\\b[^\"']*\\bvideo\\b[^\"']*[\"'][\\s\\S]*?(?=<\\/div>\\s*<\\/div>|$)", Pattern.CASE_INSENSITIVE)
        val vcMatcher = vcPattern.matcher(html)
        val searchScope = if (vcMatcher.find()) vcMatcher.group(0) ?: html else html

        val servers = mutableListOf<AnimeSaltServerEntry>()
        val iframePattern = Pattern.compile("<iframe[^>]*(?:data-src|src)=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE)
        val ifrMatcher = iframePattern.matcher(searchScope)
        var idx = 0
        while (ifrMatcher.find()) {
            var url = ifrMatcher.group(1).orEmpty().trim()
            if (url.isBlank() || url.equals("about:blank", ignoreCase = true)) continue
            if (url.startsWith("/")) {
                url = "$BASE_URL$url"
            }
            val name = serverNames.getOrNull(idx) ?: "Server ${idx + 1}"
            servers.add(AnimeSaltServerEntry(name = name, url = url))
            idx++
        }

        // Fallback: search whole HTML if scoped container found nothing
        if (servers.isEmpty()) {
            val anyIframeMatcher = iframePattern.matcher(html)
            var aidx = 0
            while (anyIframeMatcher.find()) {
                var url = anyIframeMatcher.group(1).orEmpty().trim()
                if (url.isBlank() || url.equals("about:blank", ignoreCase = true)) continue
                if (url.startsWith("/")) {
                    url = "$BASE_URL$url"
                }
                val name = serverNames.getOrNull(aidx) ?: "Server ${aidx + 1}"
                servers.add(AnimeSaltServerEntry(name = name, url = url))
                aidx++
            }
        }

        return servers
    }

    suspend fun extractServers(slug: String, isMovie: Boolean = false): AnimeSaltExtractedServers = withContext(Dispatchers.IO) {
        serversCache[slug]?.let { return@withContext it }
        val movieMode = isMovie || !slug.contains(Regex("""-\d+x\d+$"""))
        val baseMovieSlug = slug.replace(Regex("""-\d+x\d+$"""), "")
        val candidateUrls = if (movieMode) {
            listOfNotNull(
                "$BASE_URL/movies/$baseMovieSlug/",
                "$BASE_URL/anime/$baseMovieSlug/",
                if (!baseMovieSlug.endsWith("-the-movie") && !baseMovieSlug.endsWith("-movie")) "$BASE_URL/movies/$baseMovieSlug-the-movie/" else null,
                if (!baseMovieSlug.endsWith("-the-movie") && !baseMovieSlug.endsWith("-movie")) "$BASE_URL/movies/$baseMovieSlug-movie/" else null,
                if (baseMovieSlug.endsWith("-the-movie")) "$BASE_URL/movies/${baseMovieSlug.removeSuffix("-the-movie")}/" else null,
                if (baseMovieSlug.endsWith("-movie")) "$BASE_URL/movies/${baseMovieSlug.removeSuffix("-movie")}/" else null,
                "$BASE_URL/episode/$slug/"
            ).distinct()
        } else {
            listOf(
                "$BASE_URL/episode/$slug/",
                "$BASE_URL/movies/$slug/",
                "$BASE_URL/anime/$slug/"
            )
        }

        var myStream: String? = null
        var abyss: String? = null
        val allServersList = mutableListOf<AnimeSaltServerEntry>()
        var resolvedPageUrl: String? = null

        for (candidateUrl in candidateUrls) {
            try {
                val req = Request.Builder()
                    .url(candidateUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "$BASE_URL/")
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val html = resp.body?.string().orEmpty()
                    if (html.length > 200) {
                        allServersList.addAll(parseServers(html))

                        // Extract options-0 (Server 1: MyStream / as-cdn / Play)
                        val opt0Matcher = Pattern.compile(
                            "<div[^>]+id=[\"']options-0[\"'][^>]*>[\\s\\S]*?<iframe[^>]+(?:data-src|src)=[\"']([^\"']+)[\"']",
                            Pattern.CASE_INSENSITIVE
                        ).matcher(html)
                        if (opt0Matcher.find()) {
                            var u = opt0Matcher.group(1).orEmpty().trim()
                            if (u.startsWith("/")) u = "$BASE_URL$u"
                            myStream = u
                        }

                        // Extract options-1 (Server 2: Abyss / multi-lang player)
                        val opt1Matcher = Pattern.compile(
                            "<div[^>]+id=[\"']options-1[\"'][^>]*>[\\s\\S]*?<iframe[^>]+(?:data-src|src)=[\"']([^\"']+)[\"']",
                            Pattern.CASE_INSENSITIVE
                        ).matcher(html)
                        if (opt1Matcher.find()) {
                            var u = opt1Matcher.group(1).orEmpty().trim()
                            if (u.startsWith("/")) u = "$BASE_URL$u"
                            abyss = u
                        }

                        // Fallbacks if not matched by options-0/1
                        if (myStream.isNullOrBlank()) {
                            val anyAsCdn = Pattern.compile("<iframe[^>]*(?:data-src|src)=[\"']([^\"']*as-cdn[^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
                            if (anyAsCdn.find()) {
                                myStream = anyAsCdn.group(1).orEmpty().trim()
                            } else {
                                val ms = allServersList.find {
                                    it.url.contains("as-cdn", ignoreCase = true) || it.name.contains("mystream", ignoreCase = true) || it.name.contains("play", ignoreCase = true)
                                } ?: allServersList.firstOrNull { !it.url.contains("player.php", ignoreCase = true) }
                                myStream = ms?.url
                            }
                        }

                        if (abyss.isNullOrBlank()) {
                            val anyPhp = Pattern.compile("<iframe[^>]*(?:data-src|src)=[\"']([^\"']*player\\.php[^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
                            if (anyPhp.find()) {
                                abyss = anyPhp.group(1).orEmpty().trim()
                            } else {
                                val ab = allServersList.find {
                                    it.url.contains("player.php", ignoreCase = true) || it.name.contains("abyss", ignoreCase = true)
                                } ?: allServersList.getOrNull(1)
                                abyss = ab?.url
                            }
                        }

                        if (!myStream.isNullOrBlank() || !abyss.isNullOrBlank() || allServersList.isNotEmpty()) {
                            resolvedPageUrl = candidateUrl
                            break
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val finalPage = resolvedPageUrl.orEmpty()
        val hasExtractedIframe = (!myStream.isNullOrBlank() && myStream != finalPage) || (!abyss.isNullOrBlank() && abyss != finalPage)
        val result = AnimeSaltExtractedServers(
            myStreamUrl = myStream?.takeIf { it.isNotBlank() && it != finalPage },
            abyssUrl = abyss?.takeIf { it.isNotBlank() && it != finalPage },
            allServers = allServersList
        )
        if (hasExtractedIframe) {
            serversCache[slug] = result
        }
        result
    }

    suspend fun resolveEpisodeStreamUrl(
        relationship: AnimeSaltSeriesRelationship?,
        title: String,
        romajiTitle: String? = null,
        format: String? = null,
        episode: Int,
        isAbyss: Boolean = false,
        anilistId: Int = 0
    ): String {
        if (anilistId > 0 && !AnimeMappingRepository.isMapped(anilistId)) {
            return ""
        }
        val isMovie = format?.equals("MOVIE", ignoreCase = true) == true
        val slug = resolveSlug(relationship, title, romajiTitle, format, episode, anilistId)
        val servers = extractServers(slug, isMovie)
        return if (isAbyss) {
            servers.abyssUrl ?: ""
        } else {
            servers.myStreamUrl ?: ""
        }
    }

    fun resolveSlug(
        relationship: AnimeSaltSeriesRelationship?,
        title: String,
        romajiTitle: String? = null,
        format: String? = null,
        episode: Int,
        anilistId: Int = 0
    ): String {
        relationship?.episodeMap?.get(episode)?.slug?.let { return it }

        if (anilistId > 0 && AnimeMappingRepository.isMapped(anilistId)) {
            val candidate = AnimeMappingRepository.resolveCandidate(anilistId, episode, title)
            val s = candidate.season ?: 1
            return if (candidate.isMovie) candidate.saltId else generateAnimeSaltSlug(candidate.saltId, s, candidate.finalEp)
        }

        val isMovie = format?.equals("MOVIE", ignoreCase = true) == true
        val season = extractSeason(title, romajiTitle)
        val baseSlug = relationship?.normalizedTitle ?: tmdbAccurateSlugBase(title)
        return animeSaltEpisodeSlug(baseSlug, season, episode, isMovie)
    }

    fun resolveEpisodeUrl(
        relationship: AnimeSaltSeriesRelationship?,
        title: String,
        romajiTitle: String? = null,
        format: String? = null,
        episode: Int,
        isAbyss: Boolean = false,
        anilistId: Int = 0
    ): String {
        val isMovie = format?.equals("MOVIE", ignoreCase = true) == true
        val slug = resolveSlug(relationship, title, romajiTitle, format, episode, anilistId)
        val cached = serversCache[slug]
        return if (isAbyss) {
            cached?.abyssUrl ?: ""
        } else {
            cached?.myStreamUrl ?: ""
        }
    }

    private fun probeSeriesExists(slug: String): Boolean {
        return try {
            val req = Request.Builder()
                .url("$BASE_URL/series/$slug/")
                .header("User-Agent", USER_AGENT)
                .head()
                .build()
            val resp = client.newCall(req).execute()
            resp.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    private fun probeMovieExists(slug: String): Boolean {
        val paths = listOf("$BASE_URL/movies/$slug/", "$BASE_URL/anime/$slug/")
        for (p in paths) {
            try {
                val req = Request.Builder()
                    .url(p)
                    .header("User-Agent", USER_AGENT)
                    .head()
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) return true
            } catch (_: Exception) {}
        }
        return false
    }

    private fun searchAnimeSalt(query: String, isMovie: Boolean = false): String? {
        return try {
            val enc = URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder()
                .url("$BASE_URL/?s=$enc")
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val html = resp.body?.string().orEmpty()

            val patternStr = if (isMovie) {
                "href=[\"']https://animesalt\\.cx/(?:movies|anime)/([^/\"']+)/?[\"']"
            } else {
                "href=[\"']https://animesalt\\.cx/(?:series|anime)/([^/\"']+)/?[\"']"
            }
            val matcher = Pattern.compile(patternStr).matcher(html)
            val found = mutableListOf<String>()
            while (matcher.find()) {
                val s = matcher.group(1)
                if (!s.isNullOrBlank()) found.add(s)
            }

            if (found.isEmpty()) return null

            val targetNorm = normalizeAnimeSaltTitle(query)
            found.firstOrNull { it.equals(targetNorm, ignoreCase = true) }
                ?: found.firstOrNull { it.contains(targetNorm, ignoreCase = true) }
                ?: found.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads all seasons and episodes for an AnimeSalt series, building a flat, sorted index.
     */
    private suspend fun fetchAllSeriesEpisodes(
        seriesSlug: String,
        normalizedTitle: String,
        totalEpisodes: Int
    ): List<AnimeSaltEpisode> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE_URL/series/$seriesSlug/")
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val html = resp.body?.string().orEmpty()

            var postId: String? = null
            val postMatcher = Pattern.compile("data-post=[\"'](\\d+)[\"']").matcher(html)
            if (postMatcher.find()) {
                postId = postMatcher.group(1)
            }

            val seasonNumbers = mutableSetOf<Int>()
            val seasonMatcher = Pattern.compile("class=[\"'][^\"']*season-btn[^\"']*[\"'][^>]*data-season=[\"'](\\d+)[\"']").matcher(html)
            while (seasonMatcher.find()) {
                seasonMatcher.group(1)?.toIntOrNull()?.let { seasonNumbers.add(it) }
            }
            val seasonAltMatcher = Pattern.compile("data-season=[\"'](\\d+)[\"'][^>]*class=[\"'][^\"']*season-btn[^\"']*[\"']").matcher(html)
            while (seasonAltMatcher.find()) {
                seasonAltMatcher.group(1)?.toIntOrNull()?.let { seasonNumbers.add(it) }
            }
            if (seasonNumbers.isEmpty()) seasonNumbers.add(1)

            val allEpisodeSlugs = mutableListOf<String>()

            fun extractSlugs(content: String): List<String> {
                val p = Pattern.compile("href=[\"'](?:https?://animesalt\\.cx)?/episode/([^/\"\\s]+)/?[\"']", Pattern.CASE_INSENSITIVE)
                val m = p.matcher(content)
                val list = mutableListOf<String>()
                while (m.find()) {
                    val s = m.group(1).orEmpty().trim()
                    if (s.isNotBlank() && (s.startsWith(seriesSlug, ignoreCase = true) || s.startsWith(normalizedTitle, ignoreCase = true) || s.contains(seriesSlug, ignoreCase = true))) {
                        list.add(s)
                    }
                }
                return list
            }

            allEpisodeSlugs.addAll(extractSlugs(html))

            val pid = postId
            if (!pid.isNullOrBlank() && seasonNumbers.size > 1) {
                val otherSeasons = seasonNumbers.filter { it != 1 }.sorted()
                val ajaxTasks = otherSeasons.map { sNum ->
                    async {
                        try {
                            val url = "$BASE_URL/wp-admin/admin-ajax.php?action=action_select_season&season=$sNum&post=$pid"
                            val aReq = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                            val aResp = client.newCall(aReq).execute()
                            if (aResp.isSuccessful) {
                                val aHtml = aResp.body?.string().orEmpty()
                                extractSlugs(aHtml)
                            } else emptyList()
                        } catch (_: Exception) {
                            emptyList<String>()
                        }
                    }
                }
                val ajaxResults = ajaxTasks.awaitAll()
                for (res in ajaxResults) {
                    allEpisodeSlugs.addAll(res)
                }
            }

            val parsedList = mutableListOf<Triple<Int, Int, String>>()
            val seenSlugs = mutableSetOf<String>()
            val slugPattern2x = Pattern.compile("-(\\d+)[xX](\\d+)$")
            val slugPatternEp = Pattern.compile("-(?:ep|episode|-)?(\\d+)$", Pattern.CASE_INSENSITIVE)

            for (slug in allEpisodeSlugs) {
                if (!seenSlugs.add(slug)) continue
                val m2x = slugPattern2x.matcher(slug)
                if (m2x.find()) {
                    val s = m2x.group(1)?.toIntOrNull() ?: 1
                    val ep = m2x.group(2)?.toIntOrNull() ?: 1
                    parsedList.add(Triple(s, ep, slug))
                } else {
                    val mEp = slugPatternEp.matcher(slug)
                    if (mEp.find()) {
                        val ep = mEp.group(1)?.toIntOrNull() ?: 1
                        parsedList.add(Triple(1, ep, slug))
                    }
                }
            }

            parsedList.sortWith(compareBy({ it.first }, { it.second }))

            parsedList.mapIndexed { idx, (sNum, epNum, slug) ->
                AnimeSaltEpisode(
                    season = sNum,
                    episode = epNum,
                    absoluteEpisode = idx + 1,
                    title = null,
                    url = "$BASE_URL/episode/$slug/",
                    slug = slug
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Matches AniList episodes to AnimeSalt episodes:
     * 1. Per-season matching if season > 1 (e.g. Solo Leveling S2 ep 1 -> 2x1)
     * 2. Season 1 matching (ep 1 -> 1x1)
     * 3. Absolute episode matching (Hunter x Hunter ep 68 -> 4x68)
     * 4. Fallback: deterministic slug generator {normalizedTitle}-{season}x{ep}
     */
    private fun buildDeterministicEpisodeMap(
        normalizedTitle: String,
        indexedEpisodes: List<AnimeSaltEpisode>,
        targetSeason: Int,
        totalEpisodes: Int,
        isMovie: Boolean
    ): Map<Int, ResolvedEpisode> {
        val map = mutableMapOf<Int, ResolvedEpisode>()
        val maxEp = maxOf(totalEpisodes, 1)

        for (ep in 1..maxEp) {
            if (isMovie) {
                val slug = normalizeAnimeSaltTitle(normalizedTitle)
                map[ep] = ResolvedEpisode(
                    anilistEpisode = ep,
                    tmdbEpisode = ep,
                    animeSaltSeason = 1,
                    animeSaltEpisode = ep,
                    animeSaltUrl = "$BASE_URL/movies/$slug/",
                    slug = slug
                )
                continue
            }

            var matched: AnimeSaltEpisode? = null

            // 1. If targetSeason > 1, check exact season & episode
            if (targetSeason > 1) {
                matched = indexedEpisodes.find { it.season == targetSeason && it.episode == ep }
                if (matched == null) {
                    val seasonEps = indexedEpisodes.filter { it.season == targetSeason }
                    matched = seasonEps.getOrNull(ep - 1)
                }
            }

            // 2. Check season 1 exact episode
            if (matched == null && targetSeason <= 1) {
                matched = indexedEpisodes.find { it.season == 1 && it.episode == ep }
            }

            // 3. Check absoluteEpisode (for monolithic multi-season series like Hunter x Hunter)
            if (matched == null) {
                matched = indexedEpisodes.find { it.absoluteEpisode == ep }
            }

            // 4. Check any episode matching ep
            if (matched == null) {
                matched = indexedEpisodes.find { it.episode == ep }
            }

            if (matched != null) {
                map[ep] = ResolvedEpisode(
                    anilistEpisode = ep,
                    tmdbEpisode = ep,
                    animeSaltSeason = matched.season,
                    animeSaltEpisode = matched.episode,
                    animeSaltUrl = matched.url,
                    slug = matched.slug
                )
            } else {
                val slug = generateAnimeSaltSlug(normalizedTitle, targetSeason, ep)
                map[ep] = ResolvedEpisode(
                    anilistEpisode = ep,
                    tmdbEpisode = ep,
                    animeSaltSeason = targetSeason,
                    animeSaltEpisode = ep,
                    animeSaltUrl = "$BASE_URL/episode/$slug/",
                    slug = slug
                )
            }
        }

        return map
    }
}
