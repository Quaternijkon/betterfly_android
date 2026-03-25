package com.betterfly.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_types")
data class EventTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Goal fields (nullable)
    val goalType: String? = null,       // "positive" | "negative"
    val goalMetric: String? = null,     // "count" | "duration"
    val goalPeriod: String? = null,     // "week" | "month"
    val goalTargetValue: Double? = null,
    val tagsJson: String? = null        // JSON array of strings
)
