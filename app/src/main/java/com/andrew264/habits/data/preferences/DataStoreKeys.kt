package com.andrew264.habits.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKeys {
    val IS_SERVICE_ACTIVE = booleanPreferencesKey("is_service_active")
    val SELECTED_SCHEDULE_ID = stringPreferencesKey("selected_schedule_id")
}