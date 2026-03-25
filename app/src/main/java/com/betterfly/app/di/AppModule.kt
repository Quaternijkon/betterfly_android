package com.betterfly.app.di

import android.content.Context
import androidx.room.Room
import com.betterfly.app.data.local.BetterFlyDatabase
import com.betterfly.app.data.local.dao.EventTypeDao
import com.betterfly.app.data.local.dao.SessionDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): BetterFlyDatabase =
        Room.databaseBuilder(ctx, BetterFlyDatabase::class.java, "betterfly.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideEventTypeDao(db: BetterFlyDatabase): EventTypeDao = db.eventTypeDao()
    @Provides fun provideSessionDao(db: BetterFlyDatabase): SessionDao = db.sessionDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
