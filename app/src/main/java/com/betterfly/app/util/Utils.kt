package com.betterfly.app.util

import java.util.UUID
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun newId(): String = UUID.randomUUID().toString()

fun formatDuration(seconds: Long): String {
    if (seconds < 0) return "00:00:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

fun formatDurationShort(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m ${seconds % 60}s"
    }
}

fun Long.toDisplayDate(): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDisplayTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDayKey(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(this))
}

fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF4285F4)
}

fun hexToArgb(hex: String): Int = try {
    android.graphics.Color.parseColor(hex)
} catch (e: Exception) {
    0xFF4285F4.toInt()
}

fun getStartOfWeek(weekStart: Int): Long {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_WEEK)
    val firstDay = if (weekStart == 1) Calendar.MONDAY else Calendar.SUNDAY
    var diff = today - firstDay
    if (diff < 0) diff += 7
    cal.add(Calendar.DAY_OF_YEAR, -diff)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun getStartOfMonth(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
