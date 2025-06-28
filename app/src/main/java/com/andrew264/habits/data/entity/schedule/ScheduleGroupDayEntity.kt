package com.andrew264.habits.data.entity.schedule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.andrew264.habits.model.schedule.DayOfWeek

@Entity(
    tableName = "schedule_group_days",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleGroupEntity::class,
            parentColumns = ["group_id"],
            childColumns = ["group_id_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduleGroupDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "group_id_fk", index = true)
    val groupId: String,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: DayOfWeek
)