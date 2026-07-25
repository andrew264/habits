package com.andrew264.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.andrew264.habits.data.dao.*
import com.andrew264.habits.data.entity.AppUsageEventEntity
import com.andrew264.habits.data.entity.ScreenEventEntity
import com.andrew264.habits.data.entity.WaterIntakeEntry
import com.andrew264.habits.data.entity.WhitelistedAppEntity
import com.andrew264.habits.data.entity.counter.CounterEntity
import com.andrew264.habits.data.entity.counter.CounterLogEntity

@Database(
    entities = [
        WaterIntakeEntry::class,

        ScreenEventEntity::class,
        AppUsageEventEntity::class,
        WhitelistedAppEntity::class,
        CounterEntity::class,
        CounterLogEntity::class
    ],
    version = 4,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterIntakeDao(): WaterIntakeDao

    abstract fun screenEventDao(): ScreenEventDao
    abstract fun appUsageEventDao(): AppUsageEventDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun counterDao(): CounterDao

    companion object {
        const val DATABASE_NAME = "HabitsDatabase"
    }
}