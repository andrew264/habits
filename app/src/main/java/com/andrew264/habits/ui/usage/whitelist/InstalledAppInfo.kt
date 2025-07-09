package com.andrew264.habits.ui.usage.whitelist

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val friendlyName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
)