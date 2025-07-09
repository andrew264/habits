package com.andrew264.habits.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.andrew264.habits.data.entity.WhitelistedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {
    @Upsert
    suspend fun upsert(app: WhitelistedAppEntity)

    @Query("SELECT * FROM whitelisted_apps WHERE package_name = :packageName")
    fun getApp(packageName: String): Flow<WhitelistedAppEntity?>

    @Query("SELECT * FROM whitelisted_apps")
    fun getAll(): Flow<List<WhitelistedAppEntity>>

    @Query("DELETE FROM whitelisted_apps WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

}