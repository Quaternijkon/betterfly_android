package com.betterfly.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = EventTypeEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val note: String? = null,
    val incomplete: Boolean = false,
    val rating: Int? = null,   // 1-5 exertion
    val tagsJson: String? = null
)
