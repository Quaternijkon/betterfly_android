package com.betterfly.app.data.local.dao

import androidx.room.*
import com.betterfly.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE eventId = :eventId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveForEvent(eventId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(sessions: List<SessionEntity>) {
        deleteAll()
        sessions.forEach { upsert(it) }
    }
}
