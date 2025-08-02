package com.andrew264.habits.data.repository

import com.andrew264.habits.data.dao.WhitelistDao
import com.andrew264.habits.data.entity.WhitelistedAppEntity
import com.andrew264.habits.domain.model.WhitelistedApp
import com.andrew264.habits.domain.repository.WhitelistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistRepositoryImpl @Inject constructor(
    private val whitelistDao: WhitelistDao
) : WhitelistRepository {
    override suspend fun updateWhitelistedApp(app: WhitelistedApp) {
        whitelistDao.upsert(app.toEntity())
    }

    override fun getWhitelistedApps(): Flow<List<WhitelistedApp>> {
        return whitelistDao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun unWhitelistApp(packageName: String) {
        whitelistDao.delete(packageName)
    }
}

private fun WhitelistedAppEntity.toDomainModel(): WhitelistedApp {
    return WhitelistedApp(
        packageName = this.packageName,
        colorHex = this.colorHex,
        sessionLimitMinutes = this.sessionLimitMinutes
    )
}

private fun WhitelistedApp.toEntity(): WhitelistedAppEntity {
    return WhitelistedAppEntity(
        packageName = this.packageName,
        colorHex = this.colorHex,
        sessionLimitMinutes = this.sessionLimitMinutes
    )
}