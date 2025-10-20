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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.andrew264.habits.R

@Composable
fun AccessibilityServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SettingsAccessibility, contentDescription = null) },
        title = { Text(stringResource(R.string.settings_enable_service_title)) },
        text = { Text(stringResource(R.string.settings_enable_service_description)) },
        confirmButton = {
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onConfirm()
                }
            ) {
                Text(stringResource(R.string.settings_go_to_settings), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.data_management_cancel))
            }
        }
    )
}