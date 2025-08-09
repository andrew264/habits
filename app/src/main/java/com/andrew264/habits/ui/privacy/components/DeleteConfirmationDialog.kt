package com.andrew264.habits.ui.privacy.components

import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.domain.usecase.TimeRangeOption
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
internal fun DeleteConfirmationDialog(
    timeRange: TimeRangeOption,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val view = LocalView.current
    val title: String
    val text: String

    if (timeRange == TimeRangeOption.ALL_TIME) {
        title = "Delete all data?"
        text = "This will permanently delete all selected historical data from your device. This action cannot be undone."
    } else {
        title = "Delete data?"
        text = "This will permanently delete the selected data from the ${timeRange.toDisplayString(lowercase = true)}."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onConfirm()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
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

internal fun TimeRangeOption.toDisplayString(lowercase: Boolean = false): String {
    val text = when (this) {
        TimeRangeOption.LAST_HOUR -> "Last hour"
        TimeRangeOption.LAST_24_HOURS -> "Last 24 hours"
        TimeRangeOption.LAST_7_DAYS -> "Last 7 days"
        TimeRangeOption.LAST_4_WEEKS -> "Last 4 weeks"
        TimeRangeOption.ALL_TIME -> "All time"
    }
    return if (lowercase) text.lowercase() else text
}

@Preview
@Composable
private fun DeleteConfirmationDialogPreview() {
    HabitsTheme {
        DeleteConfirmationDialog(
            timeRange = TimeRangeOption.LAST_HOUR,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview
@Composable
private fun DeleteConfirmationDialogAllTimePreview() {
    HabitsTheme {
        DeleteConfirmationDialog(
            timeRange = TimeRangeOption.ALL_TIME,
            onDismiss = {},
            onConfirm = {}
        )
    }
}