package com.andrew264.habits.di

import com.andrew264.habits.data.repository.*
import com.andrew264.habits.data.scheduler.SessionAlarmSchedulerImpl
import com.andrew264.habits.data.scheduler.WaterAlarmSchedulerImpl
import com.andrew264.habits.domain.repository.*
import com.andrew264.habits.domain.scheduler.SessionAlarmScheduler
import com.andrew264.habits.domain.scheduler.WaterAlarmScheduler
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
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

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

    @Binds
    @Singleton
    abstract fun bindCounterRepository(
        counterRepositoryImpl: CounterRepositoryImpl
    ): CounterRepository

    @Binds
    @Singleton
    abstract fun bindWaterAlarmScheduler(
        waterAlarmSchedulerImpl: WaterAlarmSchedulerImpl
    ): WaterAlarmScheduler

    @Binds
    @Singleton
    abstract fun bindSessionAlarmScheduler(
        sessionAlarmSchedulerImpl: SessionAlarmSchedulerImpl
    ): SessionAlarmScheduler
}