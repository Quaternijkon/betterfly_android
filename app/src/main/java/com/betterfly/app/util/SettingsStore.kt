package com.betterfly.app.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.betterfly.app.data.PendingDeletes
import com.betterfly.app.data.PendingSyncOp
import com.betterfly.app.data.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "settings")

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val ctx: Context) {

    private object Keys {
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val WEEK_START = intPreferencesKey("week_start")
        val STOP_MODE = stringPreferencesKey("stop_mode")
        val DARK_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("dark_mode")

        val PENDING_DELETES_JSON = stringPreferencesKey("pending_deletes_json")
        val PENDING_SYNC_QUEUE_JSON = stringPreferencesKey("pending_sync_queue_json")
        val OVERVIEW_LAYOUT_JSON = stringPreferencesKey("overview_layout_json")
    }

    val settings: Flow<UserSettings> = ctx.dataStore.data.map { prefs ->
        UserSettings(
            themeColor = prefs[Keys.THEME_COLOR] ?: "#4285F4",
            weekStart = prefs[Keys.WEEK_START] ?: 1,
            stopMode = prefs[Keys.STOP_MODE] ?: "quick",
            darkMode = prefs[Keys.DARK_MODE] ?: false
        )
    }

    suspend fun save(s: UserSettings) {
        ctx.dataStore.edit { prefs ->
            prefs[Keys.THEME_COLOR] = s.themeColor
            prefs[Keys.WEEK_START] = s.weekStart
            prefs[Keys.STOP_MODE] = s.stopMode
            prefs[Keys.DARK_MODE] = s.darkMode
        }
    }

    val pendingDeletes: Flow<PendingDeletes> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[Keys.PENDING_DELETES_JSON]
        if (raw.isNullOrBlank()) PendingDeletes()
        else runCatching { json.decodeFromString(PendingDeletes.serializer(), raw) }.getOrElse { PendingDeletes() }
    }

    suspend fun setPendingDeletes(value: PendingDeletes) {
        ctx.dataStore.edit { it[Keys.PENDING_DELETES_JSON] = json.encodeToString(PendingDeletes.serializer(), value) }
    }

    suspend fun addPendingDelete(type: String, id: String) {
        val now = pendingDeletesOnce()
        val updated = when (type) {
            "sessions" -> now.copy(sessions = (now.sessions + id).distinct())
            "events" -> now.copy(events = (now.events + id).distinct())
            else -> now
        }
        setPendingDeletes(updated)
    }

    suspend fun clearPendingDeletes() {
        ctx.dataStore.edit { it.remove(Keys.PENDING_DELETES_JSON) }
    }

    val pendingSyncQueue: Flow<List<PendingSyncOp>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[Keys.PENDING_SYNC_QUEUE_JSON]
        if (raw.isNullOrBlank()) emptyList()
        else runCatching {
            json.decodeFromString(ListSerializer(PendingSyncOp.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    suspend fun setPendingSyncQueue(queue: List<PendingSyncOp>) {
        ctx.dataStore.edit {
            if (queue.isEmpty()) it.remove(Keys.PENDING_SYNC_QUEUE_JSON)
            else it[Keys.PENDING_SYNC_QUEUE_JSON] = json.encodeToString(ListSerializer(PendingSyncOp.serializer()), queue)
        }
    }

    suspend fun enqueueSync(op: PendingSyncOp) {
        val q = pendingSyncQueueOnce().toMutableList()
        q.add(op)
        setPendingSyncQueue(q)
    }

    suspend fun clearPendingSyncQueue() {
        ctx.dataStore.edit { it.remove(Keys.PENDING_SYNC_QUEUE_JSON) }
    }

    val overviewLayout: Flow<List<String>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[Keys.OVERVIEW_LAYOUT_JSON]
        if (raw.isNullOrBlank()) emptyList()
        else runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    suspend fun setOverviewLayout(layout: List<String>) {
        ctx.dataStore.edit {
            if (layout.isEmpty()) it.remove(Keys.OVERVIEW_LAYOUT_JSON)
            else it[Keys.OVERVIEW_LAYOUT_JSON] = json.encodeToString(ListSerializer(String.serializer()), layout)
        }
    }

    private suspend fun pendingDeletesOnce(): PendingDeletes =
        pendingDeletes.map { it }.firstValue()

    private suspend fun pendingSyncQueueOnce(): List<PendingSyncOp> =
        pendingSyncQueue.map { it }.firstValue()
}

private suspend fun <T> Flow<T>.firstValue(): T {
    var v: T? = null
    kotlinx.coroutines.flow.first { value ->
        v = value
        true
    }
    return v!!
}
