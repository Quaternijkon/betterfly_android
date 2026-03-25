package com.betterfly.app.data.local.dao

import androidx.room.*
import com.betterfly.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllOnce(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE eventId = :eventId ORDER BY startTime DESC")
    fun observeByEvent(eventId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endTime IS NULL LIMIT 1")
    fun observeActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE endTime IS NULL")
    suspend fun getAllActiveSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE endTime IS NULL AND eventId = :eventId LIMIT 1")
    suspend fun getActiveSession(eventId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SessionEntity>)

    @Delete
    suspend fun delete(entity: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM sessions WHERE startTime >= :from AND startTime < :to ORDER BY startTime ASC")
    suspend fun getSessionsInRange(from: Long, to: Long): List<SessionEntity>
}
