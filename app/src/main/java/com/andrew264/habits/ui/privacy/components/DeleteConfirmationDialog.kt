package com.andrew264.habits.ui.privacy.components

import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.R
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
        title = stringResource(R.string.data_management_delete_all_data_title)
        text = stringResource(R.string.data_management_delete_all_data_description)
    } else {
        title = stringResource(R.string.data_management_delete_data_title)
        text = stringResource(R.string.data_management_delete_data_description, timeRange.toDisplayString(lowercase = true))
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
                Text(stringResource(R.string.data_management_delete), fontWeight = FontWeight.Bold)
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

@Composable
internal fun TimeRangeOption.toDisplayString(lowercase: Boolean = false): String {
    val text = when (this) {
        TimeRangeOption.LAST_HOUR -> stringResource(R.string.data_management_time_range_last_hour)
        TimeRangeOption.LAST_24_HOURS -> stringResource(R.string.data_management_time_range_last_24_hours)
        TimeRangeOption.LAST_7_DAYS -> stringResource(R.string.data_management_time_range_last_7_days)
        TimeRangeOption.LAST_4_WEEKS -> stringResource(R.string.data_management_time_range_last_4_weeks)
        TimeRangeOption.ALL_TIME -> stringResource(R.string.data_management_time_range_all_time)
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