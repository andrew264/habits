package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun SettingsRow(
    text: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val view = LocalView.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(Dimens.PaddingLarge))
        Switch(
            checked = checked,
            onCheckedChange = {
                onCheckedChange(it)
                val feedback = if (it) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                view.performHapticFeedback(feedback)
            },
            enabled = enabled
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsRowPreview() {
    var checked by remember { mutableStateOf(true) }
    SettingsRow(
        text = "Setting Title",
        description = "This is a description for the setting.",
        checked = checked,
        onCheckedChange = { checked = it },
        enabled = true
    )
}
