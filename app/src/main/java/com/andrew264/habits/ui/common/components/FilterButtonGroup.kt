package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> FilterButtonGroup(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    label: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    ButtonGroup(
        modifier = modifier,
        overflowIndicator = { menuState ->
            val interactionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(interactionSource)
            IconButton(
                onClick = { menuState.show() },
                interactionSource = interactionSource
            ) {
                Icon(Icons.Default.MoreVert, stringResource(id = R.string.filter_button_group_more_options))
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
                            if (selectedOption != option) {
                                onOptionSelected(option)
                                view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                            }
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        label(option)
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { label(option) },
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
            label = { Text(it.label) }
        )
    }
}