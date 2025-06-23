package com.andrew264.habits.util

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionHandler(
    private val activity: ComponentActivity,
    private val onPermissionsHandled: (activityRecognitionGranted: Boolean, notificationsGranted: Boolean) -> Unit
) {
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val activityRecognitionGranted =
                permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true
            val postNotificationsGranted =
                permissions[Manifest.permission.POST_NOTIFICATIONS] == true
            onPermissionsHandled(activityRecognitionGranted, postNotificationsGranted)
        }

    fun requestRelevantPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        val arPermission = Manifest.permission.ACTIVITY_RECOGNITION
        val pnPermission = Manifest.permission.POST_NOTIFICATIONS

        val arCurrentlyGranted = ContextCompat.checkSelfPermission(
            activity,
            arPermission
        ) == PackageManager.PERMISSION_GRANTED
        val pnCurrentlyGranted = ContextCompat.checkSelfPermission(
            activity,
            pnPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (!arCurrentlyGranted) {
            permissionsToRequest.add(arPermission)
        }
        if (!pnCurrentlyGranted) {
            permissionsToRequest.add(pnPermission)
        }


        if (permissionsToRequest.isNotEmpty()) {
            if (!arCurrentlyGranted && activity.shouldShowRequestPermissionRationale(arPermission)) {
                Toast.makeText(
                    activity,
                    "Activity recognition is needed for accurate sleep detection. Heuristics will be used otherwise.",
                    Toast.LENGTH_LONG
                ).show()
            }
            if (!pnCurrentlyGranted && activity.shouldShowRequestPermissionRationale(pnPermission)) {
                Toast.makeText(
                    activity,
                    "Notifications permission is needed for the service to run reliably in the background.",
                    Toast.LENGTH_LONG
                ).show()
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            onPermissionsHandled(arCurrentlyGranted, pnCurrentlyGranted)
        }
    }
}