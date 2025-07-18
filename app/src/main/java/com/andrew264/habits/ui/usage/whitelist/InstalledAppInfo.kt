package com.andrew264.habits.ui.usage.whitelist

data class InstalledAppInfo(
    val packageName: String,
    val friendlyName: String,
    val isSystemApp: Boolean
)