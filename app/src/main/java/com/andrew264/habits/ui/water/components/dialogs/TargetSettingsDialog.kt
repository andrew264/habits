package com.andrew264.habits.ui.water.components.dialogs

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings

@Composable
fun TargetSettingsDialog(
    settings: PersistentSettings,
    onDismiss: () -> Unit,
    onSave: (isEnabled: Boolean, targetMl: String) -> Unit
) {
    var isEnabled by rememberSaveable { mutableStateOf(settings.isWaterTrackingEnabled) }
    var targetMl by rememberSaveable { mutableStateOf(settings.waterDailyTargetMl.toString()) }
    val view = LocalView.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingExtraLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                Text("Tracking Settings", style = MaterialTheme.typography.headlineSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Tracking", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            val feedback = if (it) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                            view.performHapticFeedback(feedback)
                        }
                    )
                }
                OutlinedTextField(
                    value = targetMl,
                    onValueChange = { targetMl = it },
                    label = { Text("Daily Target (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onDismiss()
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    }) { Text("Cancel") }
                    Spacer(Modifier.width(Dimens.PaddingSmall))
                    TextButton(onClick = {
                        onSave(isEnabled, targetMl)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Preview
@Composable
internal fun TargetSettingsDialogPreview() {
    val settings = createPreviewPersistentSettings()
    TargetSettingsDialog(settings = settings, onDismiss = {}, onSave = { _, _ -> })
}