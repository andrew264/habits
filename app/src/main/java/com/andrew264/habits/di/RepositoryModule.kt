package com.andrew264.habits.di

import com.andrew264.habits.data.repository.ScheduleRepositoryImpl
import com.andrew264.habits.data.repository.SettingsRepositoryImpl
import com.andrew264.habits.data.repository.UserPresenceHistoryRepositoryImpl
import com.andrew264.habits.data.repository.WaterRepositoryImpl
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.domain.repository.WaterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        scheduleRepositoryImpl: ScheduleRepositoryImpl
    ): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindUserPresenceHistoryRepository(
        userPresenceHistoryRepositoryImpl: UserPresenceHistoryRepositoryImpl
    ): UserPresenceHistoryRepository

    @Binds
    @Singleton
    abstract fun bindWaterRepository(
        waterRepositoryImpl: WaterRepositoryImpl
    ): WaterRepository
}