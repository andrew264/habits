package com.andrew264.habits.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object DataStoreKeys {
    val SELECTED_SCHEDULE_ID = stringPreferencesKey("selected_schedule_id")
    val BEDTIME_TRACKING_ENABLED = booleanPreferencesKey("bedtime_tracking_enabled")

    // App Usage Tracking Feature
    val APP_USAGE_TRACKING_ENABLED = booleanPreferencesKey("app_usage_tracking_enabled")
    val USAGE_LIMIT_NOTIFICATIONS_ENABLED = booleanPreferencesKey("usage_limit_notifications_enabled")

    // For tracking daily limit notifications
    val NOTIFIED_PACKAGES_DATE = stringPreferencesKey("notified_packages_date")
    val NOTIFIED_PACKAGES_LIST = stringSetPreferencesKey("notified_packages_list")

    // Water Tracking Feature
    val WATER_TRACKING_ENABLED = booleanPreferencesKey("water_tracking_enabled")
    val WATER_DAILY_TARGET_ML = intPreferencesKey("water_daily_target_ml")
    val WATER_REMINDER_ENABLED = booleanPreferencesKey("water_reminder_enabled")
    val WATER_REMINDER_INTERVAL_MINUTES = intPreferencesKey("water_reminder_interval_minutes")
    val WATER_REMINDER_SNOOZE_MINUTES = intPreferencesKey("water_reminder_snooze_minutes")
    val WATER_REMINDER_SCHEDULE_ID = stringPreferencesKey("water_reminder_schedule_id")
}