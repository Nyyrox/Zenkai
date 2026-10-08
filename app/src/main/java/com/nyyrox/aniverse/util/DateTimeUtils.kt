package com.nyyrox.aniverse.util

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    fun getCurrentIsoUtc(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.Instant.now().toString()
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.format(java.util.Date())
        }
    }

    fun parseIsoDateToMillis(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        var trimmed = isoString.trim()
        if (trimmed.contains(" ") && !trimmed.contains("T")) {
            trimmed = trimmed.replace(" ", "T")
        }
        // Normalize +00 to +00:00 if needed
        if (trimmed.endsWith("+00")) {
            trimmed = "$trimmed:00"
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    java.time.Instant.parse(trimmed).toEpochMilli()
                } catch (_: Exception) {
                    try {
                        java.time.OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        val local = java.time.LocalDateTime.parse(
                            trimmed.substringBefore("+").substringBefore("Z")
                        )
                        local.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                    }
                }
            } else {
                val clean = trimmed.substringBefore(".").substringBefore("+").substringBefore("Z")
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(clean)?.time
            }
        } catch (_: Exception) {
            try {
                val clean = trimmed.substringBefore(".").substringBefore("+").substringBefore("Z")
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(clean)?.time
            } catch (_: Exception) {
                null
            }
        }
    }

    fun isUserOnline(lastSeenAt: String?): Boolean {
        val timeMs = parseIsoDateToMillis(lastSeenAt) ?: return false
        val diffMs = Math.abs(System.currentTimeMillis() - timeMs)
        return diffMs < 5 * 60 * 1000L // 5 minutes threshold
    }

    fun formatLastSeen(lastSeenAt: String?): String {
        val timeMs = parseIsoDateToMillis(lastSeenAt) ?: return "Offline"
        val diffMs = System.currentTimeMillis() - timeMs
        if (diffMs < 0) return "Online"
        val mins = diffMs / (60 * 1000L)
        val hours = mins / 60
        val days = hours / 24
        return when {
            mins < 5 -> "Online"
            mins < 60 -> "Active ${mins}m ago"
            hours < 24 -> "Active ${hours}h ago"
            days < 7 -> "Active ${days}d ago"
            else -> "Offline"
        }
    }

    fun formatMessageTime(rawIso: String?): String {
        val millis = parseIsoDateToMillis(rawIso) ?: return "Just now"
        return try {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            sdf.format(java.util.Date(millis))
        } catch (_: Exception) {
            "Just now"
        }
    }

    fun getDayKey(rawIso: String?): String {
        val millis = parseIsoDateToMillis(rawIso) ?: return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(java.util.Date(millis))
    }

    fun formatMessageDateHeader(rawIso: String?): String {
        val millis = parseIsoDateToMillis(rawIso) ?: return "Today"
        val nowMidnight = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val msgMidnight = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val dayDiff = ((nowMidnight.timeInMillis - msgMidnight.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
        val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        val now = java.util.Calendar.getInstance()
        val isSameYear = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR)

        return when {
            dayDiff == 0 -> "Today"
            dayDiff == 1 -> "Yesterday"
            dayDiff in 2..6 -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
                dayFormat.format(msgCal.time)
            }
            isSameYear -> {
                val monthDayFormat = SimpleDateFormat("d MMMM", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
                monthDayFormat.format(msgCal.time)
            }
            else -> {
                val fullFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
                fullFormat.format(msgCal.time)
            }
        }
    }
}

