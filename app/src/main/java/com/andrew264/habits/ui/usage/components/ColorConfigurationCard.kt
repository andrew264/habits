package com.andrew264.habits.ui.usage.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.color_picker.ColorPicker
import com.andrew264.habits.ui.common.color_picker.ColorPickerState
import com.andrew264.habits.ui.common.color_picker.utils.toColor
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.usage.AppDetails

@Composable
internal fun ColorConfigurationCard(
    app: AppDetails,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val colorPickerState = rememberSaveable(app.packageName, app.color, saver = ColorPickerState.Saver) {
        ColorPickerState(initialColor = app.color.toColorOrNull() ?: Color.Gray)
    }

    val view = LocalView.current
    var hasChanges by remember { mutableStateOf(false) }

    // Track if color has changed from initial
    LaunchedEffect(colorPickerState.hsvColor) {
        hasChanges = app.color.toColorOrNull() != colorPickerState.hsvColor.toColor()
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingLarge)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Display Color", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = colorPickerState.hsvColor.toColor(),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
                    TextButton(onClick = { isExpanded = !isExpanded }) {
                        Text(if (isExpanded) "Cancel" else "Change")
                    }
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = Dimens.PaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    ColorPicker(state = colorPickerState)
                    Button(
                        onClick = {
                            onSetAppColor(app.packageName, colorPickerState.hexCode)
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            isExpanded = false
                        },
                        enabled = colorPickerState.isValidHex && hasChanges,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set Color")
                    }
                }
            }
        }
    }
}