package com.nyyrox.aniverse.data.mapping

import java.util.Locale

object AnimeSeasonMapper {

    /**
     * Resolves the real TMDB/IMDb Season number for an anime based on its AniList title,
     * romaji title, and arc naming conventions.
     */
    fun resolveSeason(title: String, romajiTitle: String? = null): Int {
        val t = "$title ${romajiTitle.orEmpty()}".lowercase(Locale.US)

        // 1. Demon Slayer (Kimetsu no Yaiba)
        if (t.contains("kimetsu no yaiba") || t.contains("demon slayer")) {
            return when {
                t.contains("hashira geiko") || t.contains("hashira training") -> 4
                t.contains("katanakaji") || t.contains("swordsmith village") -> 3
                t.contains("yuukaku") || t.contains("entertainment district") -> 2
                t.contains("mugen ressha") || t.contains("mugen train") -> 2
                else -> 1
            }
        }

        // 2. Jujutsu Kaisen
        if (t.contains("jujutsu kaisen")) {
            return when {
                t.contains("2nd season") || t.contains("season 2") || t.contains("shibuya") || t.contains("kaigyoku") -> 2
                else -> 1
            }
        }

        // 3. Attack on Titan (Shingeki no Kyojin)
        if (t.contains("shingeki no kyojin") || t.contains("attack on titan")) {
            return when {
                t.contains("final season") || t.contains("season 4") || t.contains("the final") -> 4
                t.contains("season 3") || t.contains("3rd season") -> 3
                t.contains("season 2") || t.contains("2nd season") -> 2
                else -> 1
            }
        }

        // 3b. Gintama
        if (t.contains("gintama")) {
            return when {
                t.contains("°") || t.contains("season 3") || t.contains("2015") || t.contains("degree") -> 3
                t.contains("enchousen") || t.contains("enchosan") -> 2
                t.contains("'") || t.contains("season 2") || t.contains("2011") -> 2
                t.contains("porori") -> 5
                t.contains("shirogane") || t.contains("silver soul") -> 6
                t.contains("the final") || t.contains("the semi-final") -> 7
                t.contains("season 4") || t.contains("2017") || t.contains("gintama.") -> 4
                else -> 1
            }
        }

        // 4. My Hero Academia (Boku no Hero Academia)
        if (t.contains("boku no hero academia") || t.contains("my hero academia")) {
            val seasonMatch = Regex("""(?:season\s*(\d+)|(\d+)(?:st|nd|rd|th)\s*season)""").find(t)
            if (seasonMatch != null) {
                val num = (seasonMatch.groupValues[1].ifEmpty { seasonMatch.groupValues[2] }).toIntOrNull()
                if (num != null && num in 1..20) return num
            }
        }

        // 5. Bleach
        if (t.contains("bleach")) {
            if (t.contains("thousand-year") || t.contains("sennen kessen") || t.contains("thousand year")) {
                return 17 // TMDB season for TYBW in the main Bleach series
            }
        }

        // 6. Dr. Stone
        if (t.contains("dr. stone") || t.contains("dr stone")) {
            return when {
                t.contains("science future") -> 4
                t.contains("new world") -> 3
                t.contains("stone wars") -> 2
                else -> 1
            }
        }

        // 7. Kaguya-sama: Love Is War
        if (t.contains("kaguya-sama") || t.contains("kaguya sama")) {
            return when {
                t.contains("ultra romantic") -> 3
                t.contains("season 2") || t.contains("?") -> 2
                else -> 1
            }
        }

        // 8. Sword Art Online
        if (t.contains("sword art online")) {
            return when {
                t.contains("war of underworld") || t.contains("alicization") -> 3
                t.contains("ii") || t.contains("2") -> 2
                else -> 1
            }
        }

        // 9. Black Clover
        if (t.contains("black clover")) {
            return when {
                t.contains("season 2") || t.contains("2nd season") || t.contains("ii") -> 2
                else -> 1
            }
        }

        // 6. Generic regex for Season N, Nth Season, Part N, Cour N
        val regexSeason = Regex("""\b(?:season\s*(\d+)|(\d+)(?:st|nd|rd|th)\s*season|part\s*(\d+)|cour\s*(\d+))\b""")
        val match = regexSeason.find(t)
        if (match != null) {
            val numStr = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
            val parsed = numStr?.toIntOrNull()
            if (parsed != null && parsed in 1..30) return parsed
        }

        // 7. Roman numerals at word boundaries: II -> 2, III -> 3, IV -> 4, V -> 5, VI -> 6
        if (Regex("""\b(?:ii|2nd)\b""").containsMatchIn(t)) return 2
        if (Regex("""\b(?:iii|3rd)\b""").containsMatchIn(t)) return 3
        if (Regex("""\b(?:iv|4th)\b""").containsMatchIn(t)) return 4
        if (Regex("""\b(?:v|5th)\b""").containsMatchIn(t)) return 5
        if (Regex("""\b(?:vi|6th)\b""").containsMatchIn(t)) return 6

        return 1
    }

    /**
     * Cleans an anime title to extract the primary franchise base name, stripping arc subtitles,
     * season numbers, and cour indicators so TMDB/IMDb search accurately finds the parent show.
     */
    fun cleanFranchiseTitle(title: String): String {
        var clean = title.trim()

        // Strip common arc and subtitle splitters (e.g., "Demon Slayer: Kimetsu no Yaiba - Hashira Training Arc")
        if (clean.contains(":") && !clean.startsWith("Re:")) {
            val parts = clean.split(":")
            if (parts.size >= 2) {
                val candidate = parts[0].trim()
                // If candidate is long enough and not just a prefix like "Fate", use it or keep first 2 segments
                if (candidate.length >= 6) {
                    clean = candidate
                }
            }
        }

        // Strip trailing dashes or arc names
        clean = clean
            .replace(Regex("""\s*[-–]\s*.*$"""), "")
            .replace(Regex("""\s*\(\d{4}\)\s*$"""), "")
            .replace(Regex("""\s*Final Season.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*The Final Season.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*Season\s+\d+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\d+(?:st|nd|rd|th)\s+Season.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*Part\s+\d+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*Cour\s+\d+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\b(?:II|III|IV|V|VI)\b.*$"""), "")
            .trim()

        return clean.ifBlank { title.trim() }
    }
}
