package com.andrew264.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andrew264.habits.data.converter.ScheduleTypeConverters
import com.andrew264.habits.data.dao.ScheduleDao
import com.andrew264.habits.data.dao.UserPresenceEventDao
import com.andrew264.habits.data.entity.UserPresenceEvent
import com.andrew264.habits.data.entity.schedule.ScheduleEntity

@Database(
    entities = [UserPresenceEvent::class, ScheduleEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(ScheduleTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPresenceEventDao(): UserPresenceEventDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        const val DATABASE_NAME = "HabitsDatabase"
    }
}