package com.nyyrox.aniverse.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RankTag(
    val id: String,
    val title: String,
    val minLevel: Int = 1,
    val minStreak: Int = 0,
    val colorHex: Long,
    val category: String, // "Ninja Rank", "Anime Title", "Streak Legend"
    val description: String
)

object LevelAndStreakSystem {
    // ─── EXP Rewards ───────────────────────────────────────────────────────────
    // Main income: watchtime (per real minute of video watched)
    const val EXP_WATCHTIME_PER_MIN = 20L  // 20 EXP per minute watched → 24-ep anime ≈ 500 EXP
    // Secondary income
    const val EXP_DAILY_STREAK      = 50L  // Once per calendar day login
    const val EXP_COMMUNITY_CHAT    = 5L   // Per message sent
    const val EXP_WATCHLIST_ADD     = 8L   // Per new anime added to watchlist
    const val EXP_APP_USAGE_TICK    = 3L   // Passive: per minute of browsing (non-player)
    // Legacy constant kept for any remaining references — no longer used for episodic award
    const val EXP_WATCH_EPISODE     = 0L

    // Ninja and Anime rank tags
    val ALL_TAGS = listOf(
        RankTag("novice", "Novice Otaku", minLevel = 1, colorHex = 0xFF94A3B8, category = "Ninja Rank", description = "Embarking on the anime path"),
        RankTag("academy", "Academy", minLevel = 3, colorHex = 0xFF38BDF8, category = "Ninja Rank", description = "Learning the ninja basics"),
        RankTag("genin", "Genin", minLevel = 5, colorHex = 0xFF4ADE80, category = "Ninja Rank", description = "Officially recognized Genin shinobi"),
        RankTag("demon_slayer", "Demon Slayer", minLevel = 8, colorHex = 0xFFF87171, category = "Anime Title", description = "Wielder of total concentration breathing"),
        RankTag("sorcerer", "Jujutsu Sorcerer", minLevel = 12, colorHex = 0xFFA855F7, category = "Anime Title", description = "Master of cursed energy"),
        RankTag("chunin", "Chunin", minLevel = 15, colorHex = 0xFF2DD4BF, category = "Ninja Rank", description = "Battlefield commander and tactician"),
        RankTag("saiyan", "Super Saiyan", minLevel = 20, colorHex = 0xFFFACC15, category = "Anime Title", description = "Power level bursting past 9,000"),
        RankTag("jonin", "Jonin", minLevel = 30, colorHex = 0xFF3B82F6, category = "Ninja Rank", description = "Elite ninja trusted with A-rank missions"),
        RankTag("straw_hat", "Straw Hat", minLevel = 40, colorHex = 0xFFEF4444, category = "Anime Title", description = "Sailing towards the One Piece"),
        RankTag("anbu", "ANBU", minLevel = 50, colorHex = 0xFF6366F1, category = "Ninja Rank", description = "Shadow covert ops special squad"),
        RankTag("hashira", "Hashira", minLevel = 65, colorHex = 0xFFEC4899, category = "Anime Title", description = "Pillar of supreme swordsmanship"),
        RankTag("akatsuki", "Akatsuki", minLevel = 70, colorHex = 0xFFDC2626, category = "Ninja Rank", description = "Member of the rogue cloud syndicate"),
        RankTag("sannin", "Legendary Sannin", minLevel = 80, colorHex = 0xFF8B5CF6, category = "Ninja Rank", description = "One of the three legendary shinobi"),
        RankTag("kage", "Kage", minLevel = 90, colorHex = 0xFFE11D48, category = "Ninja Rank", description = "Supreme village master and protector"),
        RankTag("hokage", "Hokage", minLevel = 100, colorHex = 0xFFFF7B00, category = "Ninja Rank", description = "The greatest shinobi in the realm"),
        RankTag("zenkai_god", "Zenkai God", minLevel = 120, colorHex = 0xFFFFD700, category = "Anime Title", description = "Transcended all anime boundaries"),

        // Streak badges
        RankTag("streak_3", "Spark", minLevel = 1, minStreak = 3, colorHex = 0xFFFB923C, category = "Streak Legend", description = "3-day watch streak"),
        RankTag("streak_7", "Blaze", minLevel = 1, minStreak = 7, colorHex = 0xFFF97316, category = "Streak Legend", description = "7-day continuous streak"),
        RankTag("streak_30", "Inferno", minLevel = 1, minStreak = 30, colorHex = 0xFFE11D48, category = "Streak Legend", description = "30-day unstoppable anime streak")
    )

    /**
     * Level calculation formula based on progressive quadratic curve:
     */
    fun calculateLevel(exp: Long): Int {
        if (exp <= 0) return 1
        var level = 1
        while (expForLevel(level + 1) <= exp) {
            level++
        }
        return level.coerceAtLeast(1)
    }

    fun expForLevel(level: Int): Long {
        if (level <= 1) return 0L
        // Quadratic curve: Lv 2 = 200, Lv 5 = 2,000, Lv 10 = 9,900, Lv 20 = 38,000, Lv 50 = 249,500, Lv 100 = ~1M
        return (level - 1L) * level.toLong() * 100L
    }

    fun expForNextLevel(level: Int): Long {
        return expForLevel(level + 1)
    }

    /**
     * Progress towards the next level as a Float between 0.0f and 1.0f.
     */
    fun calculateLevelProgress(exp: Long): Float {
        val currentLvl = calculateLevel(exp)
        val currentBase = expForLevel(currentLvl)
        val nextBase = expForNextLevel(currentLvl)
        val needed = (nextBase - currentBase).coerceAtLeast(1L)
        val gained = (exp - currentBase).coerceAtLeast(0L)
        return (gained.toFloat() / needed.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Returns the list of tags unlocked by the user's level and streak.
     */
    fun getUnlockedTags(level: Int, streak: Int): List<RankTag> {
        return ALL_TAGS.filter { tag ->
            level >= tag.minLevel && streak >= tag.minStreak
        }
    }

    /**
     * Determines default otaku rank title based on level.
     */
    fun getDefaultOtakuRank(level: Int): String {
        return when {
            level >= 100 -> "Hokage"
            level >= 90 -> "Kage"
            level >= 80 -> "Sannin"
            level >= 50 -> "ANBU"
            level >= 30 -> "Jonin"
            level >= 15 -> "Chunin"
            level >= 5 -> "Genin"
            level >= 3 -> "Academy"
            else -> "Novice"
        }
    }

    data class StreakResult(
        val newStreak: Int,
        val newLongest: Int,
        val earnedExp: Long,
        val isNewDay: Boolean,
        val todayStr: String
    )

    /**
     * Evaluates daily streak given the last active date (yyyy-MM-dd)
     */
    fun evaluateDailyStreak(
        lastActiveDateStr: String?,
        currentStreak: Int,
        longestStreak: Int
    ): StreakResult {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = Date()
        val todayStr = sdf.format(now)

        if (lastActiveDateStr == todayStr) {
            // Already logged in today
            return StreakResult(
                newStreak = currentStreak.coerceAtLeast(1),
                newLongest = longestStreak.coerceAtLeast(currentStreak).coerceAtLeast(1),
                earnedExp = 0L,
                isNewDay = false,
                todayStr = todayStr
            )
        }

        val cal = Calendar.getInstance()
        cal.time = now
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val newStreak: Int
        if (lastActiveDateStr == yesterdayStr) {
            // Consecutive day: streak incremented!
            newStreak = currentStreak + 1
        } else {
            // Missed a day or first login: restart streak at 1
            newStreak = 1
        }

        val newLongest = maxOf(longestStreak, newStreak)
        val earnedExp = EXP_DAILY_STREAK + (newStreak.coerceAtMost(20) * 2L)

        return StreakResult(
            newStreak = newStreak,
            newLongest = newLongest,
            earnedExp = earnedExp,
            isNewDay = true,
            todayStr = todayStr
        )
    }
}
