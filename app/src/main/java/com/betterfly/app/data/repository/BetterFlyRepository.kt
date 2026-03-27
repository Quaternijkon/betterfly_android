package com.betterfly.app.data.repository

import com.betterfly.app.data.EventType
import com.betterfly.app.data.Goal
import com.betterfly.app.data.Session
import com.betterfly.app.data.UserSettings
import com.betterfly.app.data.local.dao.EventTypeDao
import com.betterfly.app.data.local.dao.SessionDao
import com.betterfly.app.data.local.entity.EventTypeEntity
import com.betterfly.app.data.local.entity.SessionEntity
import com.betterfly.app.data.remote.FirestoreDataSource
import com.betterfly.app.util.SettingsStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BetterFlyRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val eventTypeDao: EventTypeDao,
    private val remote: FirestoreDataSource,
    private val settingsStore: SettingsStore,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeSessions(): Flow<List<Session>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeEventTypes(): Flow<List<EventType>> =
        eventTypeDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getActiveSession(eventId: String): Session? =
        sessionDao.getActiveForEvent(eventId)?.toDomain()

    suspend fun saveSession(session: Session) {
        sessionDao.upsert(session.toEntity())
        if (auth.currentUser != null) {
            scope.launch {
                runCatching { remote.pushSession(session) }
            }
        }
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.delete(session.toEntity())
        if (auth.currentUser != null) {
            scope.launch {
                runCatching { remote.tombstoneSession(session.id) }
            }
        }
    }

    suspend fun saveEventType(event: EventType) {
        eventTypeDao.upsert(event.toEntity())
        if (auth.currentUser != null) {
            scope.launch {
                runCatching { remote.pushEvent(event) }
            }
        }
    }

    suspend fun deleteEventType(event: EventType) {
        // Delete all sessions for this event first
        sessionDao.deleteByEventId(event.id)
        eventTypeDao.delete(event.toEntity())
        if (auth.currentUser != null) {
            scope.launch {
                runCatching {
                    remote.tombstoneEvent(event.id)
                    // Also tombstone sessions
                }
            }
        }
    }

    suspend fun archiveEvent(event: EventType, archived: Boolean) {
        val updated = event.copy(archived = archived)
        eventTypeDao.upsert(updated.toEntity())
        if (auth.currentUser != null) {
            scope.launch { runCatching { remote.pushEvent(updated) } }
        }
    }

    suspend fun updateEventGoalAndTags(eventId: String, goal: Goal?, tags: List<String>) {
        val existing = eventTypeDao.getById(eventId) ?: return
        val updated = existing.copy(
            goalType = goal?.type,
            goalMetric = goal?.metric,
            goalPeriod = goal?.period,
            goalTargetValue = goal?.targetValue,
            tags = tags.joinToString(",")
        )
        eventTypeDao.upsert(updated)
        if (auth.currentUser != null) {
            scope.launch {
                runCatching { remote.pushEvent(updated.toDomain()) }
            }
        }
    }

    suspend fun updateSettings(settings: UserSettings) {
        settingsStore.save(settings)
        if (auth.currentUser != null) {
            scope.launch { runCatching { remote.pushSettings(settings) } }
        }
    }

    /** Full bidirectional sync — fetch remote, merge with local, push local-only items */
    suspend fun fullSync(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")

        // --- Fetch remote ---
        val remoteEvents = remote.fetchEvents()
        val remoteSessions = remote.fetchSessions()
        val remoteSettings = remote.fetchSettings()

        // --- Merge events ---
        val localEvents = eventTypeDao.getAll().map { it.toDomain() }
        val remoteEventIds = remoteEvents.map { it.id }.toSet()
        val localEventIds = localEvents.map { it.id }.toSet()

        // Save all remote events locally
        remoteEvents.forEach { eventTypeDao.upsert(it.toEntity()) }

        // Push local-only events to remote
        localEvents
            .filter { it.id !in remoteEventIds }
            .forEach { remote.pushEvent(it) }

        // --- Merge sessions ---
        val localSessions = sessionDao.getAll().map { it.toDomain() }
        val remoteSessionIds = remoteSessions.map { it.id }.toSet()

        // Save all remote sessions locally
        remoteSessions.forEach { sessionDao.upsert(it.toEntity()) }

        // Push local-only sessions to remote
        localSessions
            .filter { it.id !in remoteSessionIds }
            .forEach { remote.pushSession(it) }

        // --- Merge settings ---
        if (remoteSettings != null) {
            settingsStore.save(remoteSettings)
        } else {
            remote.pushSettings(settingsStore.getCurrent())
        }
    }

    /** Overwrite all cloud data with current local state */
    suspend fun overwriteCloud(): Result<Unit> = runCatching {
        auth.currentUser?.uid ?: error("Not logged in")
        val events = eventTypeDao.getAll().map { it.toDomain() }
        val sessions = sessionDao.getAll().map { it.toDomain() }
        val settings = settingsStore.getCurrent()
        remote.overwriteAll(events, sessions, settings)
    }

    /** Pull remote data and replace local */
    suspend fun pullFromCloud(): Result<Unit> = runCatching {
        auth.currentUser?.uid ?: error("Not logged in")
        val events = remote.fetchEvents()
        val sessions = remote.fetchSessions()
        val settings = remote.fetchSettings()
        eventTypeDao.replaceAll(events.map { it.toEntity() })
        sessionDao.replaceAll(sessions.map { it.toEntity() })
        if (settings != null) settingsStore.save(settings)
    }

    // --- Entity mappers ---
    private fun Session.toEntity() = SessionEntity(
        id = id, eventId = eventId, startTime = startTime, endTime = endTime,
        note = note, incomplete = incomplete, rating = rating,
        tags = tags.joinToString(",")
    )

    private fun SessionEntity.toDomain() = Session(
        id = id, eventId = eventId, startTime = startTime, endTime = endTime,
        note = note, incomplete = incomplete, rating = rating,
        tags = if (tags.isNullOrBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    )

    private fun EventType.toEntity() = EventTypeEntity(
        id = id, name = name, color = color, archived = archived, createdAt = createdAt,
        goalType = goal?.type, goalMetric = goal?.metric,
        goalPeriod = goal?.period, goalTargetValue = goal?.targetValue,
        tags = tags.joinToString(",")
    )

    private fun EventTypeEntity.toDomain() = EventType(
        id = id, name = name, color = color, archived = archived, createdAt = createdAt,
        goal = if (goalType != null && goalMetric != null && goalPeriod != null && goalTargetValue != null)
            Goal(goalType, goalMetric, goalPeriod, goalTargetValue) else null,
        tags = if (tags.isNullOrBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    )
}
