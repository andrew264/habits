package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> FilterButtonGroup(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    getLabel: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    ButtonGroup(
        modifier = modifier,
        overflowIndicator = { menuState ->
            IconButton(onClick = { menuState.show() }) {
                Icon(Icons.Default.MoreVert, "More options")
            }
        },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            customItem(
                buttonGroupContent = {
                    ElevatedToggleButton(
                        checked = selectedOption == option,
                        onCheckedChange = {
                            onOptionSelected(option)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(getLabel(option))
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(getLabel(option)) },
                        onClick = {
                            onOptionSelected(option)
                            menuState.dismiss()
                        }
                    )
                }
            )
        }
    }
}


private enum class SampleOptions(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

@Preview(showBackground = true)
@Composable
private fun FilterButtonGroupPreview() {
    var selected by remember { mutableStateOf(SampleOptions.WEEK) }

    HabitsTheme {
        FilterButtonGroup(
            options = SampleOptions.entries,
            selectedOption = selected,
            onOptionSelected = { selected = it },
            getLabel = { it.label }
        )
    }
}