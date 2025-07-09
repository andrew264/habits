package com.andrew264.habits.di

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.andrew264.habits.data.AppDatabase
import com.andrew264.habits.data.dao.*
import com.andrew264.habits.data.entity.ScreenEventEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseCallback(dbProvider: Provider<AppDatabase>): RoomDatabase.Callback {
        return object : RoomDatabase.Callback() {
            private val scope = CoroutineScope(SupervisorJob())
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // When the database is created for the first time, insert a SCREEN_ON event
                // to establish a valid initial state for timeline tracking.
                scope.launch(Dispatchers.IO) {
                    dbProvider.get().screenEventDao().insert(
                        ScreenEventEntity(timestamp = System.currentTimeMillis(), eventType = "SCREEN_ON")
                    )
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        application: Application,
        callback: RoomDatabase.Callback
    ): AppDatabase {
        return Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addCallback(callback)
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

    @Provides
    @Singleton
    fun provideScreenEventDao(appDatabase: AppDatabase): ScreenEventDao {
        return appDatabase.screenEventDao()
    }

    @Provides
    @Singleton
    fun provideAppUsageEventDao(appDatabase: AppDatabase): AppUsageEventDao {
        return appDatabase.appUsageEventDao()
    }

    @Provides
    @Singleton
    fun provideWhitelistDao(appDatabase: AppDatabase): WhitelistDao {
        return appDatabase.whitelistDao()
    }
}