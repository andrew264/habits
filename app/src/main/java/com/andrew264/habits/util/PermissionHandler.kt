package com.andrew264.habits.util

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.andrew264.habits.R

class PermissionHandler(
    private val activity: ComponentActivity,
    private val onPermissionsHandled: (Map<String, Boolean>) -> Unit
) {
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            onPermissionsHandled(permissions)
        }

    fun requestPermissions(permissionsToRequest: List<String>) {
        val currentlyGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        if (currentlyGranted) {
            // If all are already granted, invoke the callback immediately.
            onPermissionsHandled(permissionsToRequest.associateWith { true })
        } else {
            // Show rationale and request permissions.
            permissionsToRequest.forEach { permission ->
                if (activity.shouldShowRequestPermissionRationale(permission)) {
                    val rationale = getRationaleForPermission(permission)
                    if (rationale.isNotBlank()) {
                        Toast.makeText(activity, rationale, Toast.LENGTH_LONG).show()
                    }
                }
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun getRationaleForPermission(permission: String): String {
        return when (permission) {
            android.Manifest.permission.ACTIVITY_RECOGNITION ->
                activity.getString(R.string.permission_activity_recognition_rationale)

            android.Manifest.permission.POST_NOTIFICATIONS ->
                activity.getString(R.string.permission_post_notifications_rationale)

            else -> ""
        }
    }
}