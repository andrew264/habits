package com.andrew264.habits.domain.usecase

import android.util.Log
import javax.inject.Inject

/**
 * Encapsulates the business logic for what to do after the user responds to permission requests.
 */
class HandlePermissionResultUseCase @Inject constructor() {
    fun execute(
        notificationsGranted: Boolean
    ) {
        if (!notificationsGranted) {
            Log.d(
                "HandlePermissionResultUseCase",
                "Notification permission denied. Service notifications might not show or service may not run reliably."
            )
        }
    }
}