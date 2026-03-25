package com.betterfly.app.data

import com.betterfly.app.data.local.entity.EventTypeEntity
import com.betterfly.app.data.local.entity.SessionEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private val json = Json { ignoreUnknownKeys = true }

fun EventTypeEntity.toDomain(): EventType = EventType(
    id = id,
    name = name,
    color = color,
    archived = archived,
    createdAt = createdAt,
    goal = if (goalType != null && goalMetric != null && goalPeriod != null && goalTargetValue != null) {
        Goal(type = goalType, metric = goalMetric, period = goalPeriod, targetValue = goalTargetValue)
    } else null,
    tags = tagsJson?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
)

fun EventType.toEntity(): EventTypeEntity = EventTypeEntity(
    id = id,
    name = name,
    color = color,
    archived = archived,
    createdAt = createdAt,
    goalType = goal?.type,
    goalMetric = goal?.metric,
    goalPeriod = goal?.period,
    goalTargetValue = goal?.targetValue,
    tagsJson = if (tags.isEmpty()) null else json.encodeToString(tags)
)

fun SessionEntity.toDomain(): Session = Session(
    id = id,
    eventId = eventId,
    startTime = startTime,
    endTime = endTime,
    note = note,
    incomplete = incomplete,
    rating = rating,
    tags = tagsJson?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    eventId = eventId,
    startTime = startTime,
    endTime = endTime,
    note = note,
    incomplete = incomplete,
    rating = rating,
    tagsJson = if (tags.isEmpty()) null else json.encodeToString(tags)
)
