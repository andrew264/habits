package com.andrew264.habits.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object DataStoreKeys {
    val IS_SERVICE_ACTIVE = booleanPreferencesKey("is_service_active")
    val MANUAL_BEDTIME_HOUR = intPreferencesKey("manual_bedtime_hour")
    val MANUAL_BEDTIME_MINUTE = intPreferencesKey("manual_bedtime_minute")
    val MANUAL_WAKE_UP_HOUR = intPreferencesKey("manual_wake_up_hour")
    val MANUAL_WAKE_UP_MINUTE = intPreferencesKey("manual_wake_up_minute")
}