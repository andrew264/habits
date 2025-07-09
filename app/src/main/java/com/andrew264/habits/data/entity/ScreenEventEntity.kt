package com.andrew264.habits.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_history")
data class ScreenEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "event_type")
    val eventType: String // e.g., "SCREEN_ON", "SCREEN_OFF"
)