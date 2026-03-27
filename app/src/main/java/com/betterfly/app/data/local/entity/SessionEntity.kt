package com.betterfly.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val note: String? = null,
    val incomplete: Boolean = false,
    val rating: Int? = null,
    val tagsJson: String? = null
)
