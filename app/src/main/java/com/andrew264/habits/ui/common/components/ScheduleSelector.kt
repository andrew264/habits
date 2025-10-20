package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.R
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.ui.common.list_items.ListItemPosition
import com.andrew264.habits.ui.common.list_items.SelectionListItem
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
fun ScheduleSelector(
    schedules: List<Schedule>,
    selectedSchedule: Schedule?,
    onScheduleSelected: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = stringResource(R.string.schedule_selector_label),
    position: ListItemPosition = ListItemPosition.SEPARATE
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        ScheduleSelectionDialog(
            schedules = schedules,
            selectedSchedule = selectedSchedule,
            onScheduleSelected = {
                onScheduleSelected(it)
                showDialog = false
            },
            onDismissRequest = { showDialog = false },
            title = label
        )
    }

    SelectionListItem(
        modifier = modifier,
        title = label,
        selectedValue = selectedSchedule?.name ?: stringResource(R.string.schedule_selector_none),
        onClick = { showDialog = true },
        enabled = enabled,
        position = position
    )
}

@Composable
private fun ScheduleSelectionDialog(
    schedules: List<Schedule>,
    selectedSchedule: Schedule?,
    onScheduleSelected: (Schedule) -> Unit,
    onDismissRequest: () -> Unit,
    title: String,
) {
    val view = LocalView.current
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = Dimens.PaddingLarge)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = Dimens.PaddingExtraLarge, end = Dimens.PaddingExtraLarge, bottom = Dimens.PaddingSmall)
                )
                LazyColumn {
                    items(schedules) { schedule ->
                        val isSelected = schedule.id == selectedSchedule?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onScheduleSelected(schedule)
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                                .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onScheduleSelected(schedule)
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                            )
                            Spacer(Modifier.width(Dimens.PaddingMedium))
                            Text(schedule.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
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
