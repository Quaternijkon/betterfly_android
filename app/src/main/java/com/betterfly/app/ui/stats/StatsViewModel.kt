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

fun List<Session>.countFrom(eventId: String, from: Long): Int =
    count { it.eventId == eventId && it.startTime >= from && it.endTime != null }

fun List<Session>.durationFrom(eventId: String, from: Long): Long =
    filter { it.eventId == eventId && it.startTime >= from && it.endTime != null }
        .sumOf { it.durationSeconds ?: 0L }

fun List<Session>.dailyCountMap(eventId: String): Map<String, Int> =
    filter { it.eventId == eventId && it.endTime != null }
        .groupBy { it.startTime.toDayKey() }
        .mapValues { it.value.size }

fun weeklyStart(weekStart: Int) = getStartOfWeek(weekStart)
fun monthlyStart() = getStartOfMonth()
