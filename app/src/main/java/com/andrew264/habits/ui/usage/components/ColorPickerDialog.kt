package com.andrew264.habits.ui.usage.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.AppDetails
import androidx.core.graphics.toColorInt

@Composable
fun ColorPickerDialog(
    dialogTitle: String,
    selectedColorHex: String?,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val view = LocalView.current
    val colors = remember {
        listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E", "#607D8B", "#FFFFFF"
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(dialogTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Dimens.PaddingLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    colors.chunked(5).forEach { columnColors ->
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
                            columnColors.forEach { colorHex ->
                                val isSelected = selectedColorHex?.equals(colorHex, ignoreCase = true) ?: false
                                ColorSwatch(
                                    color = Color(colorHex.toColorInt()),
                                    isSelected = isSelected,
                                    onClick = {
                                        onColorSelected(colorHex)
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            val contentColor = if (color.luminance() > 0.5) Color.Black else Color.White
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = contentColor)
        }
    }
}

private fun Color.luminance(): Float {
    return (0.2126f * red + 0.7152f * green + 0.0722f * blue)
}

@Preview(name = "Color Picker Dialog")
@Composable
private fun ColorPickerDialogPreview() {
    HabitsTheme {
        ColorPickerDialog(
            dialogTitle = "Sample App",
            selectedColorHex = "#FFFFFF",
            onDismissRequest = {},
            onColorSelected = {}
        )
    }
}

@Preview(name = "Color Picker Dialog - Color Selected")
@Composable
private fun ColorPickerDialogSelectedPreview() {
    HabitsTheme {
        ColorPickerDialog(
            dialogTitle = "Sample App",
            selectedColorHex = "#673AB7",
            onDismissRequest = {},
            onColorSelected = {}
        )
    }
}

@Preview(name = "Color Picker Dialog - Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ColorPickerDialogDarkPreview() {
    HabitsTheme(isDarkTheme = true) {
        ColorPickerDialog(
            dialogTitle = "Sample App",
            selectedColorHex = "#673AB7",
            onDismissRequest = {},
            onColorSelected = {}
        )
    }
}