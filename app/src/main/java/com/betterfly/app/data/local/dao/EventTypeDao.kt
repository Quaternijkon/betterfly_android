package com.betterfly.app.data.local.dao

import androidx.room.*
import com.betterfly.app.data.local.entity.EventTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventTypeDao {
    @Query("SELECT * FROM event_types WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<EventTypeEntity>

    @Query("SELECT * FROM event_types WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EventTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<EventTypeEntity>)

    @Delete
    suspend fun delete(entity: EventTypeEntity)

    @Query("DELETE FROM event_types WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM event_types")
    suspend fun deleteAll()
}
