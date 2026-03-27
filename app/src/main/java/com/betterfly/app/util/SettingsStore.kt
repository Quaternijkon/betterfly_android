package com.betterfly.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.betterfly.app.data.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "betterfly_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.dataStore

    private object Keys {
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val WEEK_START = intPreferencesKey("week_start")
        val STOP_MODE = stringPreferencesKey("stop_mode")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val settings: Flow<UserSettings> = ds.data.map { prefs ->
        UserSettings(
            themeColor = prefs[Keys.THEME_COLOR] ?: "#4285F4",
            weekStart = prefs[Keys.WEEK_START] ?: 1,
            stopMode = prefs[Keys.STOP_MODE] ?: "quick",
            darkMode = prefs[Keys.DARK_MODE] ?: false
        )
    }

    suspend fun getCurrent(): UserSettings = settings.first()

    suspend fun save(s: UserSettings) {
        ds.edit { prefs ->
            prefs[Keys.THEME_COLOR] = s.themeColor
            prefs[Keys.WEEK_START] = s.weekStart
            prefs[Keys.STOP_MODE] = s.stopMode
            prefs[Keys.DARK_MODE] = s.darkMode
        }
    }
}
