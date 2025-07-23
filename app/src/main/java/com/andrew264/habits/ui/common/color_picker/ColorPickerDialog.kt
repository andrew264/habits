package com.andrew264.habits.ui.common.color_picker

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.ui.common.color_picker.utils.toColor
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
fun ColorPickerDialog(
    title: String = "Select a color",
    initialColor: Color,
    showAlphaSlider: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirmation: (Color) -> Unit
) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val state = rememberSaveable(initialColor, saver = ColorPickerState.Saver) {
        ColorPickerState(initialColor)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(Dimens.PaddingLarge))

                ColorPicker(state = state, showAlphaSlider = showAlphaSlider)

                Spacer(Modifier.height(Dimens.PaddingLarge))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(56.dp)
                            .background(
                                color = state.hsvColor.toColor(),
                                shape = MaterialTheme.shapes.medium
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.medium
                            )
                    )
                    OutlinedTextField(
                        value = state.hexCode,
                        onValueChange = state::updateFromHex,
                        label = { Text("HEX") },
                        isError = !state.isValidHex,
                        supportingText = {
                            if (!state.isValidHex)
                                Text("Invalid HEX. Use #RRGGBB or #AARRGGBB")
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(Dimens.PaddingLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        }
                    ) { Text("Cancel") }

                    Spacer(Modifier.width(Dimens.PaddingSmall))

                    TextButton(
                        onClick = {
                            onConfirmation(state.hsvColor.toColor())
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                    ) { Text("OK") }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ColorPickerDialogPreview() {
    HabitsTheme {
        ColorPickerDialog(
            title = "Choose an App Color",
            initialColor = Color(0xFFE91E63),
            onDismissRequest = {},
            onConfirmation = {},
            showAlphaSlider = true
        )
    }
}

@Preview
@Composable
private fun ColorPickerDialogNoAlphaPreview() {
    HabitsTheme {
        ColorPickerDialog(
            initialColor = Color(0xFF4CAF50),
            onDismissRequest = {},
            onConfirmation = {},
            showAlphaSlider = false
        )
    }
}