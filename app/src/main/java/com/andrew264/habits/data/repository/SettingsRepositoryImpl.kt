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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
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
            val selectedScheduleId = preferences[DataStoreKeys.SELECTED_SCHEDULE_ID]
            val isBedtimeTrackingEnabled = preferences[DataStoreKeys.BEDTIME_TRACKING_ENABLED] ?: false

            // Usage
            val isAppUsageTrackingEnabled = preferences[DataStoreKeys.APP_USAGE_TRACKING_ENABLED] ?: false
            val usageLimitNotificationsEnabled = preferences[DataStoreKeys.USAGE_LIMIT_NOTIFICATIONS_ENABLED] ?: false

            // Water
            val isWaterTrackingEnabled = preferences[DataStoreKeys.WATER_TRACKING_ENABLED] ?: false
            val waterDailyTargetMl = preferences[DataStoreKeys.WATER_DAILY_TARGET_ML] ?: 2500
            val isWaterReminderEnabled = preferences[DataStoreKeys.WATER_REMINDER_ENABLED] ?: false
            val waterReminderIntervalMinutes = preferences[DataStoreKeys.WATER_REMINDER_INTERVAL_MINUTES] ?: 60
            val waterReminderSnoozeMinutes = preferences[DataStoreKeys.WATER_REMINDER_SNOOZE_MINUTES] ?: 15
            val waterReminderScheduleId = preferences[DataStoreKeys.WATER_REMINDER_SCHEDULE_ID]


            PersistentSettings(
                selectedScheduleId = selectedScheduleId,
                isBedtimeTrackingEnabled = isBedtimeTrackingEnabled,
                isAppUsageTrackingEnabled = isAppUsageTrackingEnabled,
                usageLimitNotificationsEnabled = usageLimitNotificationsEnabled,
                isWaterTrackingEnabled = isWaterTrackingEnabled,
                waterDailyTargetMl = waterDailyTargetMl,
                isWaterReminderEnabled = isWaterReminderEnabled,
                waterReminderIntervalMinutes = waterReminderIntervalMinutes,
                waterReminderSnoozeMinutes = waterReminderSnoozeMinutes,
                waterReminderScheduleId = waterReminderScheduleId
            )
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

    override suspend fun updateBedtimeTrackingEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.BEDTIME_TRACKING_ENABLED] = isEnabled
        }
    }

    override suspend fun updateAppUsageTrackingEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.APP_USAGE_TRACKING_ENABLED] = isEnabled
        }
    }

    override suspend fun updateUsageLimitNotificationsEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.USAGE_LIMIT_NOTIFICATIONS_ENABLED] = isEnabled
        }
    }


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

    override fun getNotifiedDailyPackages(): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            val today = LocalDate.now().toString()
            val storedDate = preferences[DataStoreKeys.NOTIFIED_PACKAGES_DATE]
            if (today == storedDate) {
                preferences[DataStoreKeys.NOTIFIED_PACKAGES_LIST] ?: emptySet()
            } else {
                emptySet()
            }
        }
    }

    override suspend fun addNotifiedDailyPackage(packageName: String) {
        context.dataStore.edit { settings ->
            val today = LocalDate.now().toString()
            val storedDate = settings[DataStoreKeys.NOTIFIED_PACKAGES_DATE]
            val currentSet = if (today == storedDate) {
                settings[DataStoreKeys.NOTIFIED_PACKAGES_LIST] ?: emptySet()
            } else {
                emptySet()
            }
            settings[DataStoreKeys.NOTIFIED_PACKAGES_DATE] = today
            settings[DataStoreKeys.NOTIFIED_PACKAGES_LIST] = currentSet + packageName
        }
    }
}