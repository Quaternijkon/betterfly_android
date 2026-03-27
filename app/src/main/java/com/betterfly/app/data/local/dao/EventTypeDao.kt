package com.betterfly.app.data.local.dao

import androidx.room.*
import com.betterfly.app.data.local.entity.EventTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventTypeDao {
    @Query("SELECT * FROM event_types ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types ORDER BY createdAt ASC")
    suspend fun getAll(): List<EventTypeEntity>

    @Query("SELECT * FROM event_types WHERE id = :id")
    suspend fun getById(id: String): EventTypeEntity?

    @Upsert
    suspend fun upsert(event: EventTypeEntity)

    @Delete
    suspend fun delete(event: EventTypeEntity)

    @Query("DELETE FROM event_types")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(events: List<EventTypeEntity>) {
        deleteAll()
        events.forEach { upsert(it) }
    }
}
