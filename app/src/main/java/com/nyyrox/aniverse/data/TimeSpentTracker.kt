package com.nyyrox.aniverse.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Categorized activity mode currently active in the app.
 */
enum class CurrentActivityMode {
    EXPLORING_APP,
    WATCHING_ANIME,
    READING_MANGA,
    READING_NOVEL
}

/**
 * Immutable activity time statistics holding seconds spent per media type.
 */
data class ActivityTimeStats(
    val animeSeconds: Long = 0L,
    val mangaSeconds: Long = 0L,
    val novelSeconds: Long = 0L,
    val appSeconds: Long = 0L
) {
    val totalSeconds: Long get() = (animeSeconds + mangaSeconds + novelSeconds + appSeconds).coerceAtLeast(0L)
    val totalMinutes: Long get() = totalSeconds / 60L
    val totalHours: Double get() = totalSeconds / 3600.0

    val animeRatio: Float get() = if (totalSeconds > 0) animeSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val mangaRatio: Float get() = if (totalSeconds > 0) mangaSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val novelRatio: Float get() = if (totalSeconds > 0) novelSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val appRatio: Float get() = if (totalSeconds > 0) appSeconds.toFloat() / totalSeconds.toFloat() else 0f

    fun add(mode: CurrentActivityMode, secondsToAdd: Long): ActivityTimeStats {
        if (secondsToAdd <= 0L) return this
        return when (mode) {
            CurrentActivityMode.WATCHING_ANIME -> copy(animeSeconds = animeSeconds + secondsToAdd)
            CurrentActivityMode.READING_MANGA -> copy(mangaSeconds = mangaSeconds + secondsToAdd)
            CurrentActivityMode.READING_NOVEL -> copy(novelSeconds = novelSeconds + secondsToAdd)
            CurrentActivityMode.EXPLORING_APP -> copy(appSeconds = appSeconds + secondsToAdd)
        }
    }

    /**
     * Formatted string such as "2h 45m" or "32m".
     */
    val formattedTotal: String get() = formatDuration(totalSeconds)
    val formattedAnime: String get() = formatDuration(animeSeconds)
    val formattedManga: String get() = formatDuration(mangaSeconds)
    val formattedNovel: String get() = formatDuration(novelSeconds)
    val formattedApp: String get() = formatDuration(appSeconds)

    fun formattedTotal(): String = formattedTotal
    fun formattedAnime(): String = formattedAnime
    fun formattedManga(): String = formattedManga
    fun formattedNovel(): String = formattedNovel
    fun formattedApp(): String = formattedApp

    companion object {
        fun formatDuration(seconds: Long): String {
            if (seconds <= 0L) return "0m"
            val totalMins = seconds / 60L
            val hours = totalMins / 60L
            val mins = totalMins % 60L
            return when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins}m"
            }
        }
    }
}

/**
 * Full record containing daily, weekly, and all-time buckets with automatic resets.
 */
data class TimeSpentRecord(
    val todayDate: String = getTodayDateString(),
    val daily: ActivityTimeStats = ActivityTimeStats(),
    val currentWeek: String = getCurrentWeekString(),
    val weekly: ActivityTimeStats = ActivityTimeStats(),
    val allTime: ActivityTimeStats = ActivityTimeStats()
) {
    /**
     * Checks if a new day or week has started, resetting the corresponding bucket.
     */
    fun checkAndReset(): TimeSpentRecord {
        val today = getTodayDateString()
        val week = getCurrentWeekString()
        var updated = this

        if (updated.todayDate != today) {
            updated = updated.copy(
                todayDate = today,
                daily = ActivityTimeStats()
            )
        }

        if (updated.currentWeek != week) {
            updated = updated.copy(
                currentWeek = week,
                weekly = ActivityTimeStats()
            )
        }

        return updated
    }

    /**
     * Adds elapsed seconds to daily, weekly, and all-time buckets.
     */
    fun addElapsed(mode: CurrentActivityMode, seconds: Long): TimeSpentRecord {
        val resetRecord = checkAndReset()
        return resetRecord.copy(
            daily = resetRecord.daily.add(mode, seconds),
            weekly = resetRecord.weekly.add(mode, seconds),
            allTime = resetRecord.allTime.add(mode, seconds)
        )
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun getCurrentWeekString(): String {
            val cal = Calendar.getInstance(Locale.US)
            val year = cal.get(Calendar.YEAR)
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            return "$year-W$week"
        }
    }
}

/**
 * EXP calculation rules based on time spent:
 * - Anime player: 20 EXP per minute
 * - Manga reader: 15 EXP per minute
 * - Novel reader: 15 EXP per minute
 * - Exploring & community: 3 EXP per minute
 */
object TimeSpentExpSystem {
    fun calculateExpForDuration(mode: CurrentActivityMode, seconds: Long): Long {
        val minutes = seconds / 60.0
        return when (mode) {
            CurrentActivityMode.WATCHING_ANIME -> (minutes * 20.0).toLong()
            CurrentActivityMode.READING_MANGA -> (minutes * 15.0).toLong()
            CurrentActivityMode.READING_NOVEL -> (minutes * 15.0).toLong()
            CurrentActivityMode.EXPLORING_APP -> (minutes * 3.0).toLong()
        }.coerceAtLeast(0L)
    }
}

fun TimeSpentRecord.getDailyStats(): ActivityTimeStats = daily
fun TimeSpentRecord.getWeeklyStats(): ActivityTimeStats = weekly
fun TimeSpentRecord.getAllTimeStats(): ActivityTimeStats = allTime
fun formatTimeDuration(seconds: Long): String = ActivityTimeStats.formatDuration(seconds)

