package com.andrew264.habits.di

import com.andrew264.habits.data.repository.*
import com.andrew264.habits.domain.repository.*
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

    @Binds
    @Singleton
    abstract fun bindScreenHistoryRepository(
        screenHistoryRepositoryImpl: ScreenHistoryRepositoryImpl
    ): ScreenHistoryRepository

    @Binds
    @Singleton
    abstract fun bindAppUsageRepository(
        appUsageRepositoryImpl: AppUsageRepositoryImpl
    ): AppUsageRepository

    @Binds
    @Singleton
    abstract fun bindWhitelistRepository(
        whitelistRepositoryImpl: WhitelistRepositoryImpl
    ): WhitelistRepository
}