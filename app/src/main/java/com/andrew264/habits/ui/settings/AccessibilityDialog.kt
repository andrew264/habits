package com.andrew264.habits.ui.settings

import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsAccessibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AccessibilityServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SettingsAccessibility, contentDescription = null) },
        title = { Text("Enable Service") },
        text = { Text("To track app usage, you need to enable the Habits accessibility service in your phone's settings. This allows the app to see which application is in the foreground.") },
        confirmButton = {
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onConfirm()
                }
            ) {
                Text("Go to Settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}