package com.andrew264.habits.domain.repository

import kotlinx.coroutines.flow.Flow

interface WhitelistRepository {
    suspend fun whitelistApp(
        packageName: String,
        colorHex: String
    )

    fun getWhitelistedAppsMap(): Flow<Map<String, String>>

    suspend fun unWhitelistApp(packageName: String)
}