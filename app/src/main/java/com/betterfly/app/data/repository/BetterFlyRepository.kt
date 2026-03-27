package com.betterfly.app.data.repository

import com.betterfly.app.data.EventType
import com.betterfly.app.data.Goal
import com.betterfly.app.data.Session
import com.betterfly.app.data.UserSettings
import com.betterfly.app.data.local.dao.EventTypeDao
import com.betterfly.app.data.local.dao.SessionDao
import com.betterfly.app.data.remote.FirestoreDataSource
import com.betterfly.app.data.toDomain
import com.betterfly.app.data.toEntity
import com.betterfly.app.util.SettingsStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
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
    private val json = Json { ignoreUnknownKeys = true }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() = remote.signOut()

    suspend fun signInAnonymously() = remote.signInAnonymously()

    suspend fun signInWithEmailOrRegister(email: String, password: String) =
        remote.signInWithEmailOrRegister(email, password)

    /** Placeholder — requires Activity + provider setup; throws so caller can surface a message. */
    suspend fun signInWithGoogle(): Unit = error("Google 登录需要 Activity 配合，请在 UI 层实现")

    suspend fun signInWithGithub(): Unit = error("GitHub 登录需要 OAuth provider 流程，请在 UI 层实现")

    suspend fun signInWithMicrosoft(): Unit = error("Microsoft 登录需要 OAuth provider 流程，请在 UI 层实现")

    // ── Observations ─────────────────────────────────────────────────────────

    fun observeSessions(): Flow<List<Session>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeEventTypes(): Flow<List<EventType>> =
        eventTypeDao.observeAll().map { list -> list.map { it.toDomain() } }

    // ── One-shot reads ────────────────────────────────────────────────────────

    suspend fun getAllEventsOnce(): List<EventType> =
        eventTypeDao.getAll().map { it.toDomain() }

    suspend fun getAllSessionsOnce(): List<Session> =
        sessionDao.getAll().map { it.toDomain() }

    suspend fun getActiveSession(eventId: String): Session? =
        sessionDao.getActiveForEvent(eventId)?.toDomain()

    // ── Writes ────────────────────────────────────────────────────────────────

    suspend fun saveSession(session: Session) {
        sessionDao.upsert(session.toEntity())
        if (auth.currentUser != null) {
            scope.launch { runCatching { remote.pushSession(session) } }
        }
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.delete(session.toEntity())
        if (auth.currentUser != null) {
            scope.launch { runCatching { remote.tombstoneSession(session.id) } }
        }
    }

    suspend fun saveEventType(event: EventType) {
        eventTypeDao.upsert(event.toEntity())
        if (auth.currentUser != null) {
            scope.launch { runCatching { remote.pushEvent(event) } }
        }
    }

    suspend fun deleteEventType(event: EventType) {
        sessionDao.deleteByEventId(event.id)
        eventTypeDao.delete(event.toEntity())
        if (auth.currentUser != null) {
            scope.launch { runCatching { remote.tombstoneEvent(event.id) } }
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
            tagsJson = if (tags.isEmpty()) null else json.encodeToString(tags)
        )
        eventTypeDao.upsert(updated)
        if (auth.currentUser != null) {
            scope.launch { runCatching { remote.pushEvent(updated.toDomain()) } }
        }
    }

    suspend fun updateSettings(settings: UserSettings) {
        settingsStore.save(settings)
        if (auth.currentUser != null) {
            scope.launch { runCatching { remote.pushSettings(settings) } }
        }
    }

    /** Alias used by SettingsViewModel */
    suspend fun saveSettings(settings: UserSettings) = updateSettings(settings)

    // ── Local utilities ───────────────────────────────────────────────────────

    suspend fun clearLocalData() {
        sessionDao.deleteAll()
        eventTypeDao.deleteAll()
    }

    /** Remove duplicate sessions (same eventId + startTime). Returns count removed. */
    suspend fun deduplicateLocalSessions(): Int {
        val all = sessionDao.getAll()
        val seen = mutableSetOf<String>()
        var removed = 0
        all.forEach { entity ->
            val key = "${entity.eventId}_${entity.startTime}"
            if (!seen.add(key)) {
                sessionDao.delete(entity)
                removed++
            }
        }
        return removed
    }

    // ── Cloud sync ────────────────────────────────────────────────────────────

    /** Full bidirectional sync — fetch remote, merge with local, push local-only items. */
    suspend fun fullSync(): Result<Unit> = runCatching {
        auth.currentUser?.uid ?: error("Not logged in")

        val remoteEvents = remote.fetchEvents()
        val remoteSessions = remote.fetchSessions()
        val remoteSettings = remote.fetchSettings()

        val localEvents = eventTypeDao.getAll().map { it.toDomain() }
        val remoteEventIds = remoteEvents.map { it.id }.toSet()
        remoteEvents.forEach { eventTypeDao.upsert(it.toEntity()) }
        localEvents.filter { it.id !in remoteEventIds }.forEach { remote.pushEvent(it) }

        val localSessions = sessionDao.getAll().map { it.toDomain() }
        val remoteSessionIds = remoteSessions.map { it.id }.toSet()
        remoteSessions.forEach { sessionDao.upsert(it.toEntity()) }
        localSessions.filter { it.id !in remoteSessionIds }.forEach { remote.pushSession(it) }

        if (remoteSettings != null) settingsStore.save(remoteSettings)
        else remote.pushSettings(settingsStore.getCurrent())
    }

    /** Overwrite all cloud data with current local state. */
    suspend fun overwriteCloud(): Result<Unit> = runCatching {
        auth.currentUser?.uid ?: error("Not logged in")
        val events = eventTypeDao.getAll().map { it.toDomain() }
        val sessions = sessionDao.getAll().map { it.toDomain() }
        val settings = settingsStore.getCurrent()
        remote.overwriteAll(events, sessions, settings)
    }

    /** Pull remote data and replace local. */
    suspend fun pullFromCloud(): Result<Unit> = runCatching {
        auth.currentUser?.uid ?: error("Not logged in")
        val events = remote.fetchEvents()
        val sessions = remote.fetchSessions()
        val settings = remote.fetchSettings()
        eventTypeDao.replaceAll(events.map { it.toEntity() })
        sessionDao.replaceAll(sessions.map { it.toEntity() })
        if (settings != null) settingsStore.save(settings)
    }
}
