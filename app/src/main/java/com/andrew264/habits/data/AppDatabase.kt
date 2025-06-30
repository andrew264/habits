package com.andrew264.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.andrew264.habits.data.dao.ScheduleDao
import com.andrew264.habits.data.dao.UserPresenceEventDao
import com.andrew264.habits.data.dao.WaterIntakeDao
import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.data.entity.WaterIntakeEntry
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
        WaterIntakeEntry::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPresenceEventDao(): UserPresenceEventDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun waterIntakeDao(): WaterIntakeDao

    companion object {
        const val DATABASE_NAME = "HabitsDatabase"
    }
}