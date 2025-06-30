package com.andrew264.habits.di

import android.app.Application
import androidx.room.Room
import com.andrew264.habits.data.AppDatabase
import com.andrew264.habits.data.dao.ScheduleDao
import com.andrew264.habits.data.dao.UserPresenceEventDao
import com.andrew264.habits.data.dao.WaterIntakeDao
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
            .fallbackToDestructiveMigration(false)
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

    @Provides
    @Singleton
    fun provideWaterIntakeDao(appDatabase: AppDatabase): WaterIntakeDao {
        return appDatabase.waterIntakeDao()
    }
}