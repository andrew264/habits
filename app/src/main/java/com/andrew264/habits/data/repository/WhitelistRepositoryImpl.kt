package com.andrew264.habits.data.repository

import com.andrew264.habits.data.dao.WhitelistDao
import com.andrew264.habits.data.entity.WhitelistedAppEntity
import com.andrew264.habits.domain.repository.WhitelistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistRepositoryImpl @Inject constructor(
    private val whitelistDao: WhitelistDao
) : WhitelistRepository {
    override suspend fun whitelistApp(
        packageName: String,
        colorHex: String
    ) {
        whitelistDao.upsert(
            WhitelistedAppEntity(
                packageName = packageName,
                colorHex = colorHex
            )
        )
    }

    override fun getWhitelistedAppsMap(): Flow<Map<String, String>> {
        return whitelistDao.getAll().map { entities ->
            entities.associate { it.packageName to it.colorHex }
        }
    }

    override suspend fun unWhitelistApp(packageName: String) {
        whitelistDao.delete(packageName)
    }
}