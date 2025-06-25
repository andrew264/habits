package com.andrew264.habits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_presence_history")
data class UserPresenceEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val state: String
)