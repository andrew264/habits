package com.andrew264.habits.di

import android.app.Application
import androidx.room.Room
import com.andrew264.habits.data.AppDatabase
import com.andrew264.habits.data.dao.ScheduleDao
import com.andrew264.habits.data.dao.UserPresenceEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(application: Application): AppDatabase {
        return Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(true) // Allow Room to recreate the DB on schema change
            .build()
    }

    @Provides
    @Singleton
    fun provideUserPresenceEventDao(appDatabase: AppDatabase): UserPresenceEventDao {
        return appDatabase.userPresenceEventDao()
    }

    @Provides
    @Singleton
    fun provideScheduleDao(appDatabase: AppDatabase): ScheduleDao {
        return appDatabase.scheduleDao()
    }
}