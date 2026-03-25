package com.betterfly.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betterfly.app.data.UserSettings
import com.betterfly.app.util.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsStore: SettingsStore
) : ViewModel() {
    val settings: StateFlow<UserSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())
}
