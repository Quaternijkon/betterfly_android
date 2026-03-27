package com.betterfly.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Goal
import com.betterfly.app.data.Session
import com.betterfly.app.data.UserSettings
import com.betterfly.app.data.repository.BetterFlyRepository
import com.betterfly.app.util.SettingsStore
import com.betterfly.app.util.newId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: BetterFlyRepository,
    settingsStore: SettingsStore
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val eventTypes: StateFlow<List<EventType>> = repo.observeEventTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<Session>> = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSessions: StateFlow<Map<String, Session>> = repo.observeSessions()
        .map { list -> list.filter { it.endTime == null }.associateBy { it.eventId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun startSession(event: EventType) {
        viewModelScope.launch {
            if (repo.getActiveSession(event.id) != null) return@launch
            repo.saveSession(
                Session(id = newId(), eventId = event.id, startTime = System.currentTimeMillis())
            )
        }
    }

    fun stopSession(session: Session, note: String? = null, rating: Int? = null, incomplete: Boolean = false) {
        viewModelScope.launch {
            repo.saveSession(session.copy(endTime = System.currentTimeMillis(), note = note, rating = rating, incomplete = incomplete))
        }
    }

    fun quickStop(session: Session) {
        viewModelScope.launch { repo.saveSession(session.copy(endTime = System.currentTimeMillis())) }
    }

    fun createEvent(name: String, color: String, tags: List<String> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.saveEventType(
                EventType(id = newId(), name = name.trim(), color = color, archived = false,
                    createdAt = System.currentTimeMillis(), tags = tags)
            )
        }
    }

    fun updateEvent(event: EventType) {
        viewModelScope.launch { repo.saveEventType(event) }
    }

    fun toggleArchive(event: EventType) {
        viewModelScope.launch { repo.archiveEvent(event, !event.archived) }
    }

    fun deleteEvent(event: EventType) {
        viewModelScope.launch { repo.deleteEventType(event) }
    }

    fun updateGoalAndTags(eventId: String, goal: Goal?, tags: List<String>) {
        viewModelScope.launch { repo.updateEventGoalAndTags(eventId, goal, tags) }
    }
}
