package com.andrew264.habits.domain.repository

import com.andrew264.habits.domain.model.WhitelistedApp
import kotlinx.coroutines.flow.Flow

interface WhitelistRepository {
    suspend fun updateWhitelistedApp(app: WhitelistedApp)

    fun getWhitelistedApps(): Flow<List<WhitelistedApp>>

    suspend fun unWhitelistApp(packageName: String)
}