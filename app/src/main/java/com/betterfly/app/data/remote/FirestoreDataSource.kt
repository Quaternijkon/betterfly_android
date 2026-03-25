package com.betterfly.app.data.remote

import com.betterfly.app.data.EventType
import com.betterfly.app.data.Goal
import com.betterfly.app.data.Session
import com.betterfly.app.data.UserSettings
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    private val uid get() = auth.currentUser?.uid

    private fun sessionsRef(u: String) = db.collection("users").document(u).collection("sessions")
    private fun eventsRef(u: String) = db.collection("users").document(u).collection("event_types")
    private fun settingsRef(u: String) = db.collection("users").document(u).collection("settings").document("preferences")

    fun getCurrentUser() = auth.currentUser
    fun signOut() = auth.signOut()

    suspend fun signInAnonymously() {
        auth.signInAnonymously().await()
    }

    suspend fun signInWithEmailOrRegister(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (_: Exception) {
            auth.createUserWithEmailAndPassword(email, password).await()
        }
    }

    suspend fun fetchEvents(): List<EventType> {
        val u = uid ?: return emptyList()
        return eventsRef(u).get().await().documents
            .filter { it.getBoolean("deleted") != true }
            .mapNotNull { doc ->
                runCatching {
                    val goalMap = doc.get("goal") as? Map<*, *>
                    val tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    EventType(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        color = doc.getString("color") ?: "#4285F4",
                        archived = doc.getBoolean("archived") ?: false,
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        goal = goalMap?.let {
                            Goal(
                                type = it["type"] as? String ?: return@let null,
                                metric = it["metric"] as? String ?: return@let null,
                                period = it["period"] as? String ?: return@let null,
                                targetValue = (it["targetValue"] as? Number)?.toDouble() ?: return@let null
                            )
                        },
                        tags = tags
                    )
                }.getOrNull()
            }
    }

    suspend fun fetchSessions(): List<Session> {
        val u = uid ?: return emptyList()
        return sessionsRef(u).get().await().documents
            .filter { it.getBoolean("deleted") != true }
            .mapNotNull { doc ->
                runCatching {
                    val tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    Session(
                        id = doc.id,
                        eventId = doc.getString("eventId") ?: return@runCatching null,
                        startTime = doc.getTimestamp("startTime")?.toDate()?.time ?: return@runCatching null,
                        endTime = doc.getTimestamp("endTime")?.toDate()?.time,
                        note = doc.getString("note"),
                        incomplete = doc.getBoolean("incomplete") ?: false,
                        rating = doc.getLong("rating")?.toInt(),
                        tags = tags
                    )
                }.getOrNull()
            }
    }

    suspend fun fetchSettings(): UserSettings? {
        val u = uid ?: return null
        val doc = settingsRef(u).get().await()
        if (!doc.exists()) return null
        return UserSettings(
            themeColor = doc.getString("themeColor") ?: "#4285F4",
            weekStart = doc.getLong("weekStart")?.toInt() ?: 1,
            stopMode = doc.getString("stopMode") ?: "quick",
            darkMode = doc.getBoolean("darkMode") ?: false
        )
    }

    suspend fun pushEvent(event: EventType) {
        val u = uid ?: return
        eventsRef(u).document(event.id).set(
            mapOf(
                "name" to event.name,
                "color" to event.color,
                "archived" to event.archived,
                "createdAt" to Timestamp(event.createdAt / 1000, ((event.createdAt % 1000) * 1_000_000).toInt()),
                "goal" to event.goal?.let {
                    mapOf("type" to it.type, "metric" to it.metric, "period" to it.period, "targetValue" to it.targetValue)
                },
                "tags" to event.tags
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun pushSession(session: Session) {
        val u = uid ?: return
        val data = mutableMapOf<String, Any?>(
            "eventId" to session.eventId,
            "startTime" to Timestamp(session.startTime / 1000, ((session.startTime % 1000) * 1_000_000).toInt()),
            "note" to session.note,
            "incomplete" to session.incomplete,
            "rating" to session.rating,
            "tags" to session.tags
        )
        session.endTime?.let {
            data["endTime"] = Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }
        sessionsRef(u).document(session.id).set(data, SetOptions.merge()).await()
    }

    suspend fun pushSettings(settings: UserSettings) {
        val u = uid ?: return
        settingsRef(u).set(
            mapOf(
                "themeColor" to settings.themeColor,
                "weekStart" to settings.weekStart,
                "stopMode" to settings.stopMode,
                "darkMode" to settings.darkMode
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun tombstoneSession(id: String) {
        val u = uid ?: return
        sessionsRef(u).document(id).set(mapOf("deleted" to true), SetOptions.merge()).await()
    }

    suspend fun tombstoneEvent(id: String) {
        val u = uid ?: return
        eventsRef(u).document(id).set(mapOf("deleted" to true), SetOptions.merge()).await()
    }
}
