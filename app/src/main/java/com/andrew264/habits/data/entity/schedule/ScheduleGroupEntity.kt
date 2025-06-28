package com.andrew264.habits.data.entity.schedule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_groups",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["schedule_id"],
            childColumns = ["schedule_id_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduleGroupEntity(
    @PrimaryKey
    @ColumnInfo(name = "group_id")
    val id: String,
    @ColumnInfo(name = "schedule_id_fk", index = true)
    val scheduleId: String,
    val name: String
)