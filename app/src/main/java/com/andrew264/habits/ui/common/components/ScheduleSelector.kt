package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.andrew264.habits.model.schedule.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSelector(
    schedules: List<Schedule>,
    selectedSchedule: Schedule?,
    onScheduleSelected: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Schedule"
) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (enabled) {
                expanded = !expanded
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSchedule?.name ?: "None",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            schedules.forEach { schedule ->
                DropdownMenuItem(
                    text = { Text(schedule.name) },
                    onClick = {
                        onScheduleSelected(schedule)
                        expanded = false
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                )
            }
        }
    }
}