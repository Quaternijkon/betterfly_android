package com.betterfly.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.betterfly.app.data.local.dao.EventTypeDao
import com.betterfly.app.data.local.dao.SessionDao
import com.betterfly.app.data.local.entity.EventTypeEntity
import com.betterfly.app.data.local.entity.SessionEntity

@Database(entities = [EventTypeEntity::class, SessionEntity::class], version = 1, exportSchema = false)
abstract class BetterFlyDatabase : RoomDatabase() {
    abstract fun eventTypeDao(): EventTypeDao
    abstract fun sessionDao(): SessionDao
}
