package com.andrew264.habits.di

import android.app.Application
import android.content.Context
import com.andrew264.habits.domain.controller.UserPresenceController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module(includes = [DatabaseModule::class])
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application
    }

    @Provides
    @Singleton
    fun provideUserPresenceController(context: Context): UserPresenceController {
        return UserPresenceController(context)
    }

}