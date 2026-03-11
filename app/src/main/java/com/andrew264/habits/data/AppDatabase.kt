package com.andrew264.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.andrew264.habits.data.dao.*
import com.andrew264.habits.data.entity.*
import com.andrew264.habits.data.entity.counter.CounterEntity
import com.andrew264.habits.data.entity.counter.CounterLogEntity
import com.andrew264.habits.data.entity.schedule.ScheduleEntity
import com.andrew264.habits.data.entity.schedule.ScheduleGroupDayEntity
import com.andrew264.habits.data.entity.schedule.ScheduleGroupEntity
import com.andrew264.habits.data.entity.schedule.ScheduleTimeRangeEntity

@Database(
    entities = [
        UserPresenceEvent::class,
        ScheduleEntity::class,
        ScheduleGroupEntity::class,
        ScheduleGroupDayEntity::class,
        ScheduleTimeRangeEntity::class,
        WaterIntakeEntry::class,

        ScreenEventEntity::class,
        AppUsageEventEntity::class,
        WhitelistedAppEntity::class,
        CounterEntity::class,
        CounterLogEntity::class
    ],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPresenceEventDao(): UserPresenceEventDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun waterIntakeDao(): WaterIntakeDao

    abstract fun screenEventDao(): ScreenEventDao
    abstract fun appUsageEventDao(): AppUsageEventDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun counterDao(): CounterDao

    companion object {
        const val DATABASE_NAME = "HabitsDatabase"
    }
}