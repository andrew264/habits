package com.andrew264.habits.data.converter

import androidx.room.TypeConverter
import com.andrew264.habits.model.schedule.ScheduleGroup
import kotlinx.serialization.json.Json

class ScheduleTypeConverters {
    @TypeConverter
    fun fromGroupList(groups: List<ScheduleGroup>): String {
        return Json.encodeToString(groups)
    }

    @TypeConverter
    fun toGroupList(jsonString: String): List<ScheduleGroup> {
        return Json.decodeFromString<List<ScheduleGroup>>(jsonString)
    }
}