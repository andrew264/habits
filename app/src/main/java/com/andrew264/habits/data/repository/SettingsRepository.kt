package com.andrew264.habits.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.andrew264.habits.data.preferences.DataStoreKeys
import com.andrew264.habits.model.ManualSleepSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "habits_settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    val settingsFlow: Flow<PersistentSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isServiceActive = preferences[DataStoreKeys.IS_SERVICE_ACTIVE] ?: false
            val manualBedtimeHour = preferences[DataStoreKeys.MANUAL_BEDTIME_HOUR]
            val manualBedtimeMinute = preferences[DataStoreKeys.MANUAL_BEDTIME_MINUTE]
            val manualWakeUpHour = preferences[DataStoreKeys.MANUAL_WAKE_UP_HOUR]
            val manualWakeUpMinute = preferences[DataStoreKeys.MANUAL_WAKE_UP_MINUTE]

            val manualSleepSchedule = ManualSleepSchedule(
                bedtimeHour = manualBedtimeHour,
                bedtimeMinute = manualBedtimeMinute,
                wakeUpHour = manualWakeUpHour,
                wakeUpMinute = manualWakeUpMinute
            )
            PersistentSettings(isServiceActive, manualSleepSchedule)
        }

    suspend fun updateServiceActiveState(isActive: Boolean) {
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.IS_SERVICE_ACTIVE] = isActive
        }
    }

    suspend fun updateManualBedtime(hour: Int?, minute: Int?) {
        context.dataStore.edit { settings ->
            if (hour != null && minute != null) {
                settings[DataStoreKeys.MANUAL_BEDTIME_HOUR] = hour
                settings[DataStoreKeys.MANUAL_BEDTIME_MINUTE] = minute
            } else {
                settings.remove(DataStoreKeys.MANUAL_BEDTIME_HOUR)
                settings.remove(DataStoreKeys.MANUAL_BEDTIME_MINUTE)
            }
        }
    }

    suspend fun updateManualWakeUpTime(hour: Int?, minute: Int?) {
        context.dataStore.edit { settings ->
            if (hour != null && minute != null) {
                settings[DataStoreKeys.MANUAL_WAKE_UP_HOUR] = hour
                settings[DataStoreKeys.MANUAL_WAKE_UP_MINUTE] = minute
            } else {
                settings.remove(DataStoreKeys.MANUAL_WAKE_UP_HOUR)
                settings.remove(DataStoreKeys.MANUAL_WAKE_UP_MINUTE)
            }
        }
    }
}