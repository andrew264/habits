package com.andrew264.habits.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.andrew264.habits.service.AppUsageAccessibilityService

object AccessibilityUtils {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = "${context.packageName}/${AppUsageAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
        stringColonSplitter.setString(enabledServices)
        while (stringColonSplitter.hasNext()) {
            val componentName = stringColonSplitter.next()
            if (componentName.equals(service, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}