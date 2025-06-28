package com.andrew264.habits.data.entity.schedule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey
    @ColumnInfo(name = "schedule_id")
    val id: String,
    val name: String,
)