package com.betterfly.app.data.repository

import com.betterfly.app.data.EventType
import com.betterfly.app.data.PendingSyncOp
import com.betterfly.app.data.Session
import com.betterfly.app.data.UserSettings
import com.betterfly.app.data.local.dao.EventTypeDao
import com.betterfly.app.data.local.dao.SessionDao
import com.betterfly.app.data.remote.FirestoreDataSource
import com.betterfly.app.data.toDomain
import com.betterfly.app.data.toEntity
import com.betterfly.app.data.toWire
import com.betterfly.app.util.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BetterFlyRepository @Inject constructor(
    private val eventTypeDao: EventTypeDao,
    private val sessionDao: SessionDao,
    private val settingsStore: SettingsStore,
    private val remote: FirestoreDataSource
) {
    fun observeEventTypes(): Flow<List<EventType>> =
        eventTypeDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeSessions(): Flow<List<Session>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun saveEventType(event: EventType) {
        eventTypeDao.upsert(event.toEntity())
        settingsStore.enqueueSync(PendingSyncOp.EventUpdate(event.toWire()))
        runCatching { remote.pushEvent(event) }.onSuccess { consumeQueueBestEffort() }
    }

    suspend fun archiveEvent(event: EventType, archived: Boolean) {
        saveEventType(event.copy(archived = archived))
    }

    suspend fun updateEventGoalAndTags(eventId: String, goal: com.betterfly.app.data.Goal?, tags: List<String>) {
        val current = eventTypeDao.getById(eventId)?.toDomain() ?: return
        saveEventType(current.copy(goal = goal, tags = tags.distinct().filter { it.isNotBlank() }))
    }

    suspend fun deleteEventType(event: EventType) {
        eventTypeDao.deleteById(event.id)
        settingsStore.addPendingDelete("events", event.id)
        settingsStore.enqueueSync(PendingSyncOp.EventDelete(event.id))
        runCatching { remote.tombstoneEvent(event.id) }.onSuccess { consumeQueueBestEffort() }
    }

    suspend fun saveSession(session: Session) {
        sessionDao.upsert(session.toEntity())
        settingsStore.enqueueSync(PendingSyncOp.SessionUpdate(session.toWire()))
        runCatching { remote.pushSession(session) }.onSuccess { consumeQueueBestEffort() }
    }

    suspend fun updateSession(session: Session) {
        saveSession(session)
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.deleteById(session.id)
        settingsStore.addPendingDelete("sessions", session.id)
        settingsStore.enqueueSync(PendingSyncOp.SessionDelete(session.id))
        runCatching { remote.tombstoneSession(session.id) }.onSuccess { consumeQueueBestEffort() }
    }

    suspend fun getActiveSession(eventId: String): Session? =
        sessionDao.getActiveSession(eventId)?.toDomain()

    suspend fun getAllActiveSessions(): List<Session> =
        sessionDao.getAllActiveSessions().map { it.toDomain() }

    suspend fun getAllEventsOnce(): List<EventType> =
        eventTypeDao.getAllOnce().map { it.toDomain() }

    suspend fun getAllSessionsOnce(): List<Session> =
        sessionDao.getAllOnce().map { it.toDomain() }

    suspend fun saveSettings(settings: UserSettings) {
        settingsStore.save(settings)
        settingsStore.enqueueSync(PendingSyncOp.SettingsUpdate(settings.toWire()))
        runCatching { remote.pushSettings(settings) }.onSuccess { consumeQueueBestEffort() }
    }

    suspend fun syncFromCloud(): Boolean {
        if (remote.getCurrentUser() == null) return false
        return try {
            val remoteEvents = remote.fetchEvents()
            val remoteSessions = remote.fetchSessions()
            val remoteSettings = remote.fetchSettings()

            val dedupedSessions = dedupeSessions(remoteSessions)

            eventTypeDao.deleteAll()
            if (remoteEvents.isNotEmpty()) eventTypeDao.upsertAll(remoteEvents.map { it.toEntity() })

            sessionDao.deleteAll()
            if (dedupedSessions.isNotEmpty()) sessionDao.upsertAll(dedupedSessions.map { it.toEntity() })

            if (remoteSettings != null) settingsStore.save(remoteSettings)

            consumeQueueBestEffort()
            settingsStore.clearPendingDeletes()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun overwriteCloudWithLocal(): Boolean {
        if (remote.getCurrentUser() == null) return false
        return try {
            val localEvents = getAllEventsOnce()
            val localSessions = getAllSessionsOnce()
            val localSettings = settingsStore.settings.first()

            localEvents.forEach { remote.pushEvent(it) }
            localSessions.forEach { remote.pushSession(it) }
            remote.pushSettings(localSettings)

            settingsStore.clearPendingSyncQueue()
            settingsStore.clearPendingDeletes()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearLocalData() {
        sessionDao.deleteAll()
        eventTypeDao.deleteAll()
    }

    suspend fun deduplicateLocalSessions(): Int {
        val all = getAllSessionsOnce()
        val deduped = dedupeSessions(all)
        val removed = all.size - deduped.size
        if (removed > 0) {
            sessionDao.deleteAll()
            sessionDao.upsertAll(deduped.map { it.toEntity() })
        }
        return removed
    }

    suspend fun consumeQueueBestEffort() {
        if (remote.getCurrentUser() == null) return
        val queue = settingsStore.pendingSyncQueue.first()
        if (queue.isEmpty()) return

        val remaining = mutableListOf<PendingSyncOp>()
        queue.forEach { op ->
            val ok = runCatching {
                when (op) {
                    is PendingSyncOp.EventCreate -> remote.pushEvent(op.payload.toDomain())
                    is PendingSyncOp.EventUpdate -> remote.pushEvent(op.payload.toDomain())
                    is PendingSyncOp.EventDelete -> remote.tombstoneEvent(op.id)
                    is PendingSyncOp.SessionCreate -> remote.pushSession(op.payload.toDomain())
                    is PendingSyncOp.SessionUpdate -> remote.pushSession(op.payload.toDomain())
                    is PendingSyncOp.SessionDelete -> remote.tombstoneSession(op.id)
                    is PendingSyncOp.SettingsUpdate -> remote.pushSettings(op.payload.toDomain())
                }
            }.isSuccess
            if (!ok) remaining.add(op)
        }
        settingsStore.setPendingSyncQueue(remaining)
    }

    private fun dedupeSessions(items: List<Session>): List<Session> {
        val byId = linkedMapOf<String, Session>()
        items.forEach { s ->
            val existed = byId[s.id]
            if (existed == null || s.startTime >= existed.startTime) byId[s.id] = s
        }
        val activeByEvent = mutableSetOf<String>()
        val signature = mutableSetOf<String>()
        val sorted = byId.values.sortedByDescending { it.startTime }
        val out = mutableListOf<Session>()
        sorted.forEach { s ->
            if (s.endTime == null) {
                if (activeByEvent.contains(s.eventId)) return@forEach
                activeByEvent.add(s.eventId)
            }
            val sig = "${s.eventId}-${s.startTime}-${s.endTime ?: ""}-${s.note ?: ""}-${if (s.incomplete) 1 else 0}"
            if (signature.contains(sig)) return@forEach
            signature.add(sig)
            out.add(s)
        }
        return out
    }

    suspend fun signInAnonymously() {
        remote.signInAnonymously()
        consumeQueueBestEffort()
        syncFromCloud()
    }

    suspend fun signInWithEmailOrRegister(email: String, password: String) {
        remote.signInWithEmailOrRegister(email, password)
        consumeQueueBestEffort()
        syncFromCloud()
    }

    // OAuth placeholders: Android 端需配合 ActivityResult/OAuth provider UI flow。
    suspend fun signInWithGoogle() { throw UnsupportedOperationException("Google OAuth UI flow pending") }
    suspend fun signInWithGithub() { throw UnsupportedOperationException("GitHub OAuth UI flow pending") }
    suspend fun signInWithMicrosoft() { throw UnsupportedOperationException("Microsoft OAuth UI flow pending") }

    fun getCurrentUser() = remote.getCurrentUser()
    fun signOut() = remote.signOut()
}
