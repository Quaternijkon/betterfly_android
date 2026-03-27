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
    val avgGapSec: Double = 0.0
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
    count { it.eventId == eventId && it.startTime >= from && it.endTime != null }

fun List<Session>.durationFrom(eventId: String, from: Long): Long =
    filter { it.eventId == eventId && it.startTime >= from && it.endTime != null }
        .sumOf { it.durationSeconds ?: 0L }

fun List<Session>.dailyCountMap(eventId: String): Map<String, Int> =
    filter { it.eventId == eventId && it.endTime != null }
        .groupBy { it.startTime.toDayKey() }
        .mapValues { it.value.size }

fun List<Session>.dailyDurationMap(eventId: String): Map<String, Long> =
    filter { it.eventId == eventId && it.endTime != null }
        .groupBy { it.startTime.toDayKey() }
        .mapValues { entry -> entry.value.sumOf { it.durationSeconds ?: 0L } }

fun List<Session>.overviewStats(eventId: String): OverviewStats {
    val completed = filter { it.eventId == eventId && it.endTime != null }
    if (completed.isEmpty()) return OverviewStats()
    val durations = completed.map { it.durationSeconds ?: 0L }.filter { it > 0 }.sorted()
    val sorted = completed.sortedBy { it.startTime }
    val gaps = mutableListOf<Long>()
    for (i in 1 until sorted.size) {
        val gap = (sorted[i].startTime - (sorted[i - 1].endTime ?: sorted[i - 1].startTime)) / 1000L
        if (gap > 0) gaps.add(gap)
    }
    return OverviewStats(
        totalCount = completed.size,
        totalDurationSec = durations.sum(),
        maxDurationSec = durations.maxOrNull() ?: 0L,
        minDurationSec = durations.minOrNull() ?: 0L,
        avgDurationSec = if (durations.isEmpty()) 0.0 else durations.average(),
        medianDurationSec = median(durations.map { it.toDouble() }) ?: 0.0,
        p90DurationSec = percentile(durations.map { it.toDouble() }, 0.90) ?: 0.0,
        minGapSec = gaps.minOrNull() ?: 0L,
        avgGapSec = if (gaps.isEmpty()) 0.0 else gaps.average()
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
    val cal = java.util.Calendar.getInstance()
    filter { it.eventId == eventId && it.endTime != null }.forEach { s ->
        cal.timeInMillis = s.startTime
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        out[h] = (out[h] ?: 0L) + (s.durationSeconds ?: 0L)
    }
    return out
}

fun weeklyStart(weekStart: Int) = getStartOfWeek(weekStart)
fun monthlyStart() = getStartOfMonth()
