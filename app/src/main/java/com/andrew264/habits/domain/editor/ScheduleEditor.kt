package com.andrew264.habits.domain.editor

import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.model.schedule.TimeRange
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A stateless helper class that contains functions for modifying a Schedule object.
 */
@Singleton
class ScheduleEditor @Inject constructor() {

    fun updateScheduleName(
        schedule: Schedule,
        name: String
    ): Schedule {
        return schedule.copy(name = name)
    }

    // --- Group Management ---

    fun addGroup(schedule: Schedule): Schedule {
        val newGroup = ScheduleGroup(
            id = UUID.randomUUID().toString(),
            name = "New Group",
            days = emptySet(),
            timeRanges = emptyList()
        )
        return schedule.copy(groups = schedule.groups + newGroup)
    }

    fun deleteGroup(
        schedule: Schedule,
        groupId: String
    ): Schedule {
        return schedule.copy(
            groups = schedule.groups.filterNot { it.id == groupId }
        )
    }

    fun updateGroupName(
        schedule: Schedule,
        groupId: String,
        newName: String
    ): Schedule {
        return schedule.copy(
            groups = schedule.groups.map { group ->
                if (group.id == groupId) group.copy(name = newName) else group
            }
        )
    }

    fun toggleDayInGroup(
        schedule: Schedule,
        groupId: String,
        day: DayOfWeek
    ): Schedule {
        return schedule.copy(
            groups = schedule.groups.map { group ->
                if (group.id == groupId) {
                    val newDays = if (day in group.days) {
                        group.days - day
                    } else {
                        group.days + day
                    }
                    group.copy(days = newDays)
                } else {
                    group
                }
            }
        )
    }

    fun addTimeRangeToGroup(
        schedule: Schedule,
        groupId: String,
        timeRange: TimeRange
    ): Schedule {
        return schedule.copy(
            groups = schedule.groups.map { group ->
                if (group.id == groupId) {
                    group.copy(timeRanges = (group.timeRanges + timeRange).sortedBy { it.fromMinuteOfDay })
                } else {
                    group
                }
            }
        )
    }

    fun updateTimeRangeInGroup(
        schedule: Schedule,
        groupId: String,
        updatedTimeRange: TimeRange
    ): Schedule {
        return schedule.copy(
            groups = schedule.groups.map { group ->
                if (group.id == groupId) {
                    group.copy(
                        timeRanges = group.timeRanges.map { if (it.id == updatedTimeRange.id) updatedTimeRange else it }
                            .sortedBy { it.fromMinuteOfDay }
                    )
                } else {
                    group
                }
            }
        )
    }

    fun deleteTimeRangeFromGroup(
        schedule: Schedule,
        groupId: String,
        timeRange: TimeRange
    ): Schedule {
        return schedule.copy(
            groups = schedule.groups.map { group ->
                if (group.id == groupId) {
                    group.copy(timeRanges = group.timeRanges.filterNot { it.id == timeRange.id })
                } else {
                    group
                }
            }
        )
    }

    // --- Per-Day View Logic ---

    fun addTimeRangeToDay(
        schedule: Schedule,
        day: DayOfWeek,
        timeRange: TimeRange
    ): Schedule {
        // Try to find an existing group that *only* contains this day
        val singleDayGroup = schedule.groups.find { it.days == setOf(day) }

        return if (singleDayGroup != null) {
            // Add to existing single-day group
            schedule.copy(
                groups = schedule.groups.map { group ->
                    if (group.id == singleDayGroup.id) {
                        group.copy(
                            timeRanges = (group.timeRanges + timeRange).sortedBy { it.fromMinuteOfDay })
                    } else {
                        group
                    }
                }
            )
        } else {
            // Create a new group for this day
            val newGroup = ScheduleGroup(
                id = UUID.randomUUID().toString(),
                name = day.name.lowercase().replaceFirstChar { it.titlecase() },
                days = setOf(day),
                timeRanges = listOf(timeRange)
            )
            schedule.copy(groups = schedule.groups + newGroup)
        }
    }

    fun updateTimeRangeInDay(
        schedule: Schedule,
        day: DayOfWeek,
        updatedTimeRange: TimeRange
    ): ScheduleModificationResult {
        val sourceGroup = schedule.groups.find { group ->
            day in group.days && group.timeRanges.any { it.id == updatedTimeRange.id }
        } ?: return ScheduleModificationResult(schedule, null)

        // Case 1: Simple update, no un-grouping needed.
        if (sourceGroup.days.size == 1) {
            val newSchedule = schedule.copy(
                groups = schedule.groups.map { group ->
                    if (group.id == sourceGroup.id) {
                        group.copy(
                            timeRanges = group.timeRanges.map { if (it.id == updatedTimeRange.id) updatedTimeRange else it }
                                .sortedBy { it.fromMinuteOfDay })
                    } else {
                        group
                    }
                }
            )
            return ScheduleModificationResult(newSchedule, null)
        }

        // Case 2: Un-grouping is required.
        val unGroupMessage = "${day.name.lowercase().replaceFirstChar { it.titlecase() }} schedule is now separate from '${sourceGroup.name}' group."

        val newGroupForDay = ScheduleGroup(
            id = UUID.randomUUID().toString(),
            name = day.name.lowercase().replaceFirstChar { it.titlecase() },
            days = setOf(day),
            timeRanges = sourceGroup.timeRanges.map { if (it.id == updatedTimeRange.id) updatedTimeRange else it }
                .sortedBy { it.fromMinuteOfDay }
        )

        val updatedOriginalGroup = sourceGroup.copy(days = sourceGroup.days - day)

        val newGroupsList = schedule.groups
            .filterNot { it.id == sourceGroup.id } + updatedOriginalGroup + newGroupForDay

        val newSchedule = schedule.copy(groups = newGroupsList.filterNot { it.days.isEmpty() })
        return ScheduleModificationResult(newSchedule, unGroupMessage)
    }

    fun deleteTimeRangeFromDay(
        schedule: Schedule,
        day: DayOfWeek,
        timeRangeToDelete: TimeRange
    ): ScheduleModificationResult {
        val sourceGroup = schedule.groups.find { group ->
            day in group.days && group.timeRanges.any { it.id == timeRangeToDelete.id }
        } ?: return ScheduleModificationResult(schedule, null)

        // Case 1: The group only affects this day.
        if (sourceGroup.days.size == 1) {
            val newSchedule = schedule.copy(
                groups = schedule.groups.map { group ->
                    if (group.id == sourceGroup.id) {
                        group.copy(timeRanges = group.timeRanges.filterNot { it.id == timeRangeToDelete.id })
                    } else {
                        group
                    }
                }.filterNot { it.timeRanges.isEmpty() && it.days.isEmpty() }
            )
            return ScheduleModificationResult(newSchedule, null)
        }

        // Case 2: The group affects multiple days. We must un-group.
        val unGroupMessage = "${day.name.lowercase().replaceFirstChar { it.titlecase() }} schedule is now separate from '${sourceGroup.name}' group."

        val newGroupForDay = ScheduleGroup(
            id = UUID.randomUUID().toString(),
            name = day.name.lowercase().replaceFirstChar { it.titlecase() },
            days = setOf(day),
            timeRanges = sourceGroup.timeRanges.filterNot { it.id == timeRangeToDelete.id }
        )

        val updatedOriginalGroup = sourceGroup.copy(days = sourceGroup.days - day)

        val newGroupsList = schedule.groups
            .filterNot { it.id == sourceGroup.id } + updatedOriginalGroup + newGroupForDay

        val newSchedule = schedule.copy(groups = newGroupsList.filterNot { it.days.isEmpty() && it.timeRanges.isEmpty() })
        return ScheduleModificationResult(newSchedule, unGroupMessage)
    }
}

/**
 * A result class for modification operations that might produce a side effect, like a message for the user.
 */
data class ScheduleModificationResult(
    val schedule: Schedule,
    val userMessage: String?
)