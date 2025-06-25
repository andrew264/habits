package com.andrew264.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.andrew264.habits.data.dao.UserPresenceEventDao
import com.andrew264.habits.data.entity.UserPresenceEvent

@Database(entities = [UserPresenceEvent::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPresenceEventDao(): UserPresenceEventDao

    companion object {
        const val DATABASE_NAME = "HabitsDatabase"
    }
}