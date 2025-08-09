package com.andrew264.habits.ui.privacy.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.domain.usecase.TimeRangeOption
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
internal fun TimeRangeRow(
    selected: TimeRangeOption,
    onSelected: (TimeRangeOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Time range", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))

        Box {
            Row(
                modifier = Modifier
                    .clickable {
                        expanded = true
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    .padding(vertical = Dimens.PaddingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
            ) {
                Text(
                    text = selected.toDisplayString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select time range"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TimeRangeOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toDisplayString()) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeRangeRowPreview() {
    HabitsTheme {
        TimeRangeRow(selected = TimeRangeOption.LAST_HOUR, onSelected = {})
    }
}