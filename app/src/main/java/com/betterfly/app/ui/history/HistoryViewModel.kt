package com.betterfly.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Session
import com.betterfly.app.data.repository.BetterFlyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: BetterFlyRepository
) : ViewModel() {

    val eventTypes: StateFlow<List<EventType>> = repo.observeEventTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<Session>> = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteSession(session: Session) {
        viewModelScope.launch { repo.deleteSession(session) }
    }

    fun updateSession(session: Session) {
        viewModelScope.launch { repo.saveSession(session) }
    }
}
