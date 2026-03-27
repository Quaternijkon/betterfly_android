package com.betterfly.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Session
import com.betterfly.app.data.repository.BetterFlyRepository
import com.betterfly.app.util.getStartOfMonth
import com.betterfly.app.util.getStartOfWeek
import com.betterfly.app.util.toDayKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class OverviewStats(
    val totalCount: Int = 0,
    val totalDurationSec: Long = 0L,
    val maxDurationSec: Long = 0L,
    val minDurationSec: Long = 0L,
    val avgDurationSec: Double = 0.0,
    val medianDurationSec: Double = 0.0,
    val p90DurationSec: Double = 0.0,
    val minGapSec: Long = 0L,
    val avgGapSec: Double = 0.0,
    val currentStreak: Int = 0,
    val currentGap: Int = 0,
    val maxStreak: Int = 0,
    val maxGap: Int = 0
)

data class StatsUiState(
    val events: List<EventType> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val weekStart: Int = 1
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    repo: BetterFlyRepository
) : ViewModel() {
    val uiState: StateFlow<StatsUiState> = combine(
        repo.observeEventTypes(),
        repo.observeSessions()
    ) { events, sessions ->
        StatsUiState(
            events = events.filterNot { it.archived },
            sessions = sessions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}

fun median(values: List<Double>): Double? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
}

fun percentile(values: List<Double>, p: Double): Double? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val idx = (p * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
    return sorted[idx]
}

fun List<Session>.countFrom(eventId: String, from: Long): Int =
    count { it.eventId == eventId && it.startTime >= from && it.endTime != null && !it.incomplete }

fun List<Session>.durationFrom(eventId: String, from: Long): Long =
    filter { it.eventId == eventId && it.startTime >= from && it.endTime != null }
        .sumOf { it.durationSeconds ?: 0L }

fun List<Session>.dailyCountMap(eventId: String): Map<String, Int> =
    filter { it.eventId == eventId && it.endTime != null && !it.incomplete }
        .groupBy { it.startTime.toDayKey() }
        .mapValues { it.value.size }

fun List<Session>.dailyDurationMap(eventId: String): Map<String, Long> =
    filter { it.eventId == eventId && it.endTime != null }
        .groupBy { it.startTime.toDayKey() }
        .mapValues { entry -> entry.value.sumOf { it.durationSeconds ?: 0L } }

/** streak / gap in days based on unique session days */
fun List<Session>.streakStats(eventId: String): Triple<Int, Int, Int> /* streak, gap, maxStreak */ {
    val completed = filter { it.eventId == eventId && it.endTime != null && !it.incomplete }
    if (completed.isEmpty()) return Triple(0, 0, 0)
    val dayKeys = completed.map { it.startTime.toDayKey() }.toSortedSet().toList()
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    fun diffDays(a: String, b: String): Int {
        val dA = sdf.parse(a) ?: return 0
        val dB = sdf.parse(b) ?: return 0
        return ((dB.time - dA.time) / 86400_000L).toInt()
    }
    var maxStreak = 1; var tempStreak = 1
    for (i in 1 until dayKeys.size) {
        if (diffDays(dayKeys[i - 1], dayKeys[i]) == 1) tempStreak++ else { maxStreak = maxOf(maxStreak, tempStreak); tempStreak = 1 }
    }
    maxStreak = maxOf(maxStreak, tempStreak)
    val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val todayKey = sdf.format(today.time)
    val yesterdayKey = sdf.format(java.util.Date(today.timeInMillis - 86400_000L))
    val last = dayKeys.last()
    var streak = 0; var gap = 0
    if (last == todayKey || last == yesterdayKey) {
        streak = 1
        var check = sdf.parse(last)!!.time - 86400_000L
        while (dayKeys.contains(sdf.format(java.util.Date(check)))) { streak++; check -= 86400_000L }
    } else {
        gap = diffDays(last, todayKey)
    }
    return Triple(streak, gap, maxStreak)
}

/** durations series for CDWI (持续) */
fun List<Session>.buildDurationSeries(eventId: String): List<Long> =
    filter { it.eventId == eventId && it.endTime != null && !it.incomplete }
        .sortedBy { it.startTime }
        .mapNotNull { it.durationSeconds }

/** wait series: gap from end of previous to start of next (wait time) */
fun List<Session>.buildWaitSeries(eventId: String): List<Long> {
    val sorted = filter { it.eventId == eventId && it.endTime != null }.sortedBy { it.startTime }
    return (1 until sorted.size).mapNotNull { i ->
        val gap = (sorted[i].startTime - (sorted[i - 1].endTime ?: sorted[i - 1].startTime)) / 1000L
        if (gap >= 0) gap else null
    }
}

/** interval series: start-to-start */
fun List<Session>.buildIntervalSeries(eventId: String): List<Long> {
    val sorted = filter { it.eventId == eventId && it.endTime != null }.sortedBy { it.startTime }
    return (1 until sorted.size).map { i -> (sorted[i].startTime - sorted[i - 1].startTime) / 1000L }
}

/** cycle series: end-to-start of next (= wait + next duration) */
fun List<Session>.buildCycleSeries(eventId: String): List<Long> {
    val sorted = filter { it.eventId == eventId && it.endTime != null }.sortedBy { it.startTime }
    return (1 until sorted.size).mapNotNull { i ->
        val dur = sorted[i].durationSeconds ?: return@mapNotNull null
        val wait = (sorted[i].startTime - (sorted[i - 1].endTime ?: return@mapNotNull null)) / 1000L
        if (wait >= 0) wait + dur else null
    }
}

fun List<Session>.overviewStats(eventId: String): OverviewStats {
    val completed = filter { it.eventId == eventId && it.endTime != null && !it.incomplete }
    val all = filter { it.eventId == eventId && it.endTime != null }
    if (all.isEmpty()) return OverviewStats()
    val durations = completed.map { it.durationSeconds ?: 0L }.filter { it > 0 }.sorted()
    val sorted = all.sortedBy { it.startTime }
    val gaps = mutableListOf<Long>()
    for (i in 1 until sorted.size) {
        val gap = (sorted[i].startTime - (sorted[i - 1].endTime ?: sorted[i - 1].startTime)) / 1000L
        if (gap > 0) gaps.add(gap)
    }
    val (streak, gap, maxStreak) = this.streakStats(eventId)
    val gapDays = if (gap > 0) gap else 0
    val maxGap = (1 until sorted.size).maxOfOrNull { i ->
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val a = sdf.format(java.util.Date(sorted[i - 1].startTime))
        val b = sdf.format(java.util.Date(sorted[i].startTime))
        val da = sdf.parse(a)?.time ?: 0L
        val db = sdf.parse(b)?.time ?: 0L
        ((db - da) / 86400_000L).toInt()
    } ?: 0
    return OverviewStats(
        totalCount = completed.size,
        totalDurationSec = durations.sum(),
        maxDurationSec = durations.maxOrNull() ?: 0L,
        minDurationSec = durations.minOrNull() ?: 0L,
        avgDurationSec = if (durations.isEmpty()) 0.0 else durations.average(),
        medianDurationSec = median(durations.map { it.toDouble() }) ?: 0.0,
        p90DurationSec = percentile(durations.map { it.toDouble() }, 0.90) ?: 0.0,
        minGapSec = gaps.minOrNull() ?: 0L,
        avgGapSec = if (gaps.isEmpty()) 0.0 else gaps.average(),
        currentStreak = streak,
        currentGap = gapDays,
        maxStreak = maxStreak,
        maxGap = maxGap
    )
}

fun List<Session>.dailyDurationSeries(eventId: String, days: Int): List<Long> {
    val endMs = System.currentTimeMillis()
    val startMs = endMs - days * 86400_000L
    val buckets = LongArray(days)
    filter { it.eventId == eventId && it.endTime != null && it.startTime in startMs..endMs }
        .forEach { s ->
            val idx = ((s.startTime - startMs) / 86400_000L).toInt().coerceIn(0, days - 1)
            buckets[idx] += s.durationSeconds ?: 0L
        }
    return buckets.toList()
}

fun List<Session>.hourlyDurationMap(eventId: String): Map<Int, Long> {
    val out = mutableMapOf<Int, Long>()
    val cal = Calendar.getInstance()
    filter { it.eventId == eventId && it.endTime != null }.forEach { s ->
        cal.timeInMillis = s.startTime
        val h = cal.get(Calendar.HOUR_OF_DAY)
        out[h] = (out[h] ?: 0L) + (s.durationSeconds ?: 0L)
    }
    return out
}

fun statSummary(values: List<Long>): StatSummary? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val avg = sorted.average()
    val mid = sorted.size / 2
    val p50 = if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid].toDouble()
    val p99idx = (sorted.size * 0.99).toInt().coerceIn(0, sorted.size - 1)
    return StatSummary(min = sorted.first(), max = sorted.last(), avg = avg, p50 = p50, p99 = sorted[p99idx])
}

data class StatSummary(val min: Long, val max: Long, val avg: Double, val p50: Double, val p99: Long)

fun weeklyStart(weekStart: Int) = getStartOfWeek(weekStart)
fun monthlyStart() = getStartOfMonth()
