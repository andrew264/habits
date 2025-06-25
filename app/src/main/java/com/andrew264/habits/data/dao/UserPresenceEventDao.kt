package com.andrew264.habits.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrew264.habits.data.entity.UserPresenceEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPresenceEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: UserPresenceEvent)

    /**
     * Gets all events, ordered by timestamp descending (most recent first).
     */
    @Query("SELECT * FROM user_presence_history ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<UserPresenceEvent>>

    /**
     * Gets all events from a specific start time, ordered by timestamp ascending.
     * Useful for constructing timelines.
     */
    @Query("SELECT * FROM user_presence_history WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getEventsFromFlow(startTime: Long): Flow<List<UserPresenceEvent>>

    /**
     * Gets all events within a specific time range, ordered by timestamp ascending.
     */
    @Query("SELECT * FROM user_presence_history WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    fun getEventsInRangeFlow(
        startTime: Long,
        endTime: Long
    ): Flow<List<UserPresenceEvent>>

    /**
     * Gets the most recent event before a given timestamp.
     * Useful for finding the state at the beginning of a period if no event falls exactly at startTime.
     */
    @Query("SELECT * FROM user_presence_history WHERE timestamp < :startTime ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEventBefore(startTime: Long): UserPresenceEvent?
}