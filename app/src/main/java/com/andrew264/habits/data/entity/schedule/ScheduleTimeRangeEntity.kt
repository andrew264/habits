package com.andrew264.habits.data.entity.schedule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_time_ranges",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleGroupEntity::class,
            parentColumns = ["group_id"],
            childColumns = ["group_id_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduleTimeRangeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "group_id_fk", index = true)
    val groupId: String,
    @ColumnInfo(name = "from_minute_of_day")
    val fromMinuteOfDay: Int,
    @ColumnInfo(name = "to_minute_of_day")
    val toMinuteOfDay: Int
)