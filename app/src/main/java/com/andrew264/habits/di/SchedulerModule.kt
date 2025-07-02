package com.andrew264.habits.di

import com.andrew264.habits.data.scheduler.WaterAlarmSchedulerImpl
import com.andrew264.habits.domain.scheduler.WaterAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {
    @Binds
    @Singleton
    abstract fun bindWaterAlarmScheduler(
        waterAlarmSchedulerImpl: WaterAlarmSchedulerImpl
    ): WaterAlarmScheduler
}