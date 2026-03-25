package com.betterfly.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Goal(
    val type: String,
    val metric: String,
    val period: String,
    val targetValue: Double
)

data class EventType(
    val id: String,
    val name: String,
    val color: String,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val goal: Goal? = null,
    val tags: List<String> = emptyList()
)

data class Session(
    val id: String,
    val eventId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val note: String? = null,
    val incomplete: Boolean = false,
    val rating: Int? = null,
    val tags: List<String> = emptyList()
) {
    val isActive: Boolean get() = endTime == null
    val durationMillis: Long? get() = endTime?.let { it - startTime }
    val durationSeconds: Long? get() = durationMillis?.div(1000L)
}

data class UserSettings(
    val themeColor: String = "#4285F4",
    val weekStart: Int = 1,
    val stopMode: String = "quick", // quick | note | interactive
    val darkMode: Boolean = false
)

@Serializable
data class PendingDeletes(
    val sessions: List<String> = emptyList(),
    val events: List<String> = emptyList()
)

@Serializable
sealed class PendingSyncOp {
    @Serializable data class SessionCreate(val payload: SessionWire) : PendingSyncOp()
    @Serializable data class SessionUpdate(val payload: SessionWire) : PendingSyncOp()
    @Serializable data class SessionDelete(val id: String) : PendingSyncOp()
    @Serializable data class EventCreate(val payload: EventTypeWire) : PendingSyncOp()
    @Serializable data class EventUpdate(val payload: EventTypeWire) : PendingSyncOp()
    @Serializable data class EventDelete(val id: String) : PendingSyncOp()
    @Serializable data class SettingsUpdate(val payload: UserSettingsWire) : PendingSyncOp()
}

@Serializable
data class EventTypeWire(
    val id: String,
    val name: String,
    val color: String,
    val archived: Boolean,
    val createdAt: Long,
    val goal: Goal? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class SessionWire(
    val id: String,
    val eventId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val note: String? = null,
    val incomplete: Boolean = false,
    val rating: Int? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class UserSettingsWire(
    val themeColor: String,
    val weekStart: Int,
    val stopMode: String,
    val darkMode: Boolean
)

@Serializable
data class BackupPayload(
    val events: List<EventTypeWire>,
    val sessions: List<SessionWire>,
    val settings: UserSettingsWire
)

fun EventType.toWire() = EventTypeWire(id, name, color, archived, createdAt, goal, tags)
fun EventTypeWire.toDomain() = EventType(id, name, color, archived, createdAt, goal, tags)

fun Session.toWire() = SessionWire(id, eventId, startTime, endTime, note, incomplete, rating, tags)
fun SessionWire.toDomain() = Session(id, eventId, startTime, endTime, note, incomplete, rating, tags)

fun UserSettings.toWire() = UserSettingsWire(themeColor, weekStart, stopMode, darkMode)
fun UserSettingsWire.toDomain() = UserSettings(themeColor, weekStart, stopMode, darkMode)
