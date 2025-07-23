package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.ui.theme.HabitsTheme

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
    var expanded by rememberSaveable { mutableStateOf(false) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ScheduleSelectorPreview() {
    val schedules = listOf(
        Schedule(
            id = "1",
            name = "Schedule 1",
            groups = listOf(
                ScheduleGroup(id = "1", name = "Group 1", days = setOf(), timeRanges = listOf())
            )
        ),
        Schedule(
            id = "2",
            name = "Schedule 2",
            groups = listOf(
                ScheduleGroup(id = "2", name = "Group 2", days = setOf(), timeRanges = listOf())
            )
        )
    )
    var selectedSchedule by remember { mutableStateOf<Schedule?>(null) }
    HabitsTheme {
        ScheduleSelector(
            schedules = schedules,
            selectedSchedule = selectedSchedule,
            onScheduleSelected = { selectedSchedule = it })
    }
}