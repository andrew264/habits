package com.andrew264.habits.data.entity.schedule

import androidx.room.Embedded
import androidx.room.Relation
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.model.schedule.TimeRange

data class GroupWithRelations(
    @Embedded val group: ScheduleGroupEntity,
    @Relation(
        parentColumn = "group_id",
        entityColumn = "group_id_fk"
    )
    val days: List<ScheduleGroupDayEntity>,
    @Relation(
        parentColumn = "group_id",
        entityColumn = "group_id_fk"
    )
    val timeRanges: List<ScheduleTimeRangeEntity>
) {
    fun toDomainModel(): ScheduleGroup {
        return ScheduleGroup(
            id = this.group.id,
            name = this.group.name,
            days = this.days.map { it.dayOfWeek }.toSet(),
            timeRanges = this.timeRanges.map { it.toDomainModel() }
        )
    }

    private fun ScheduleTimeRangeEntity.toDomainModel(): TimeRange {
        return TimeRange(
            fromMinuteOfDay = this.fromMinuteOfDay,
            toMinuteOfDay = this.toMinuteOfDay
        )
    }
}

data class ScheduleWithRelations(
    @Embedded val schedule: ScheduleEntity,
    @Relation(
        entity = ScheduleGroupEntity::class,
        parentColumn = "schedule_id",
        entityColumn = "schedule_id_fk"
    )
    val groupsWithRelations: List<GroupWithRelations>
) {
    fun toDomainModel(): Schedule {
        return Schedule(
            id = this.schedule.id,
            name = this.schedule.name,
            groups = this.groupsWithRelations.map { it.toDomainModel() }
        )
    }
}