package com.andrew264.habits.data.entity.schedule

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrew264.habits.model.schedule.ScheduleGroup

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val groups: List<ScheduleGroup>
)