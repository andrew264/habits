package com.andrew264.habits.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.andrew264.habits.data.preferences.DataStoreKeys
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.model.schedule.DefaultSchedules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "habits_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(@param:ApplicationContext private val context: Context) : SettingsRepository {

    override val settingsFlow: Flow<PersistentSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isServiceActive = preferences[DataStoreKeys.IS_SERVICE_ACTIVE] == true
            val selectedScheduleId = preferences[DataStoreKeys.SELECTED_SCHEDULE_ID]

            // Water Tracking Settings
            val isWaterTrackingEnabled = preferences[DataStoreKeys.WATER_TRACKING_ENABLED] == true
            val waterDailyTargetMl = preferences[DataStoreKeys.WATER_DAILY_TARGET_ML] ?: 2500
            val isWaterReminderEnabled = preferences[DataStoreKeys.WATER_REMINDER_ENABLED] == true
            val waterReminderIntervalMinutes = preferences[DataStoreKeys.WATER_REMINDER_INTERVAL_MINUTES] ?: 60
            val waterReminderSnoozeMinutes = preferences[DataStoreKeys.WATER_REMINDER_SNOOZE_MINUTES] ?: 15
            val waterReminderScheduleId = preferences[DataStoreKeys.WATER_REMINDER_SCHEDULE_ID] ?: DefaultSchedules.DEFAULT_SLEEP_SCHEDULE_ID


            PersistentSettings(
                isServiceActive = isServiceActive,
                selectedScheduleId = selectedScheduleId,
                isWaterTrackingEnabled = isWaterTrackingEnabled,
                waterDailyTargetMl = waterDailyTargetMl,
                isWaterReminderEnabled = isWaterReminderEnabled,
                waterReminderIntervalMinutes = waterReminderIntervalMinutes,
                waterReminderSnoozeMinutes = waterReminderSnoozeMinutes,
                waterReminderScheduleId = waterReminderScheduleId
            )
        }

    override suspend fun updateServiceActiveState(isActive: Boolean) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.IS_SERVICE_ACTIVE] = isActive
        }
    }

    override suspend fun updateSelectedScheduleId(scheduleId: String?) {
        context.dataStore.edit { settings ->
            if (scheduleId != null) {
                settings[DataStoreKeys.SELECTED_SCHEDULE_ID] = scheduleId
            } else {
                settings.remove(DataStoreKeys.SELECTED_SCHEDULE_ID)
            }
        }
    }

    // --- Water Tracking Settings Updaters ---

    override suspend fun updateWaterTrackingEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.WATER_TRACKING_ENABLED] = isEnabled
        }
    }

    override suspend fun updateWaterDailyTarget(targetMl: Int) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.WATER_DAILY_TARGET_ML] = targetMl
        }
    }

    override suspend fun updateWaterReminderEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.WATER_REMINDER_ENABLED] = isEnabled
        }
    }

    override suspend fun updateWaterReminderInterval(minutes: Int) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.WATER_REMINDER_INTERVAL_MINUTES] = minutes
        }
    }

    override suspend fun updateWaterReminderSnoozeTime(minutes: Int) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.WATER_REMINDER_SNOOZE_MINUTES] = minutes
        }
    }

    override suspend fun updateWaterReminderSchedule(scheduleId: String) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.WATER_REMINDER_SCHEDULE_ID] = scheduleId
        }
    }
}