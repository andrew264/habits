package com.andrew264.habits.ui.usage.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.color_picker.ColorPickerDialog
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.color_picker.utils.toHexCode
import com.andrew264.habits.ui.common.dialogs.DurationPickerDialog
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.usage.AppDetails

@Composable
fun AppCustomizationSection(
    app: AppDetails,
    onSaveLimits: (dailyMinutes: Int?, sessionMinutes: Int?) -> Unit,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(title = "LIMITS")
        LimitSettingsRow(
            title = "Daily limit",
            limitMinutes = app.dailyLimitMinutes,
            onSaveLimit = { newLimit -> onSaveLimits(newLimit, app.sessionLimitMinutes) }
        )
        HorizontalDivider()
        LimitSettingsRow(
            title = "Session limit",
            limitMinutes = app.sessionLimitMinutes,
            onSaveLimit = { newLimit -> onSaveLimits(app.dailyLimitMinutes, newLimit) }
        )

        SettingsSectionHeader(title = "APPEARANCE")
        ColorSettingsRow(
            app = app,
            onSetAppColor = onSetAppColor
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Dimens.PaddingLarge, bottom = Dimens.PaddingSmall)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LimitSettingsRow(
    title: String,
    limitMinutes: Int?,
    onSaveLimit: (Int?) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        DurationPickerDialog(
            title = "Set ${title.replaceFirstChar { it.titlecase() }}",
            description = "Set a time limit for this app. The limit will reset automatically. Set to 0 to clear.",
            initialTotalMinutes = limitMinutes ?: 0,
            onDismissRequest = { showDialog = false },
            onConfirm = { totalMinutes ->
                onSaveLimit(if (totalMinutes > 0) totalMinutes else null)
                showDialog = false
            }
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    HapticInteractionEffect(interactionSource)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                showDialog = true
            }
            .padding(vertical = Dimens.PaddingMedium)
    ) {
        Text(title.replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.headlineSmallEmphasized)
        Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
        Text(
            text = if (limitMinutes != null) FormatUtils.formatDuration(limitMinutes * 60_000L) else "Not set",
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorSettingsRow(
    app: AppDetails,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        ColorPickerDialog(
            title = "Choose color for ${app.friendlyName}",
            initialColor = app.color.toColorOrNull() ?: Color.Gray,
            showAlphaSlider = false,
            onDismissRequest = { showDialog = false },
            onConfirmation = { newColor ->
                onSetAppColor(app.packageName, newColor.toHexCode(includeAlpha = false))
                showDialog = false
            }
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    HapticInteractionEffect(interactionSource)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                showDialog = true
            }
            .padding(vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Display color", style = MaterialTheme.typography.headlineSmallEmphasized)
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = app.color
                        .toColorOrNull() ?: Color.Gray,
                    shape = MaterialTheme.shapes.small
                )
        )
    }
}