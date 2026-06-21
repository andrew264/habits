package com.andrew264.habits.ui.common.components.list

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.components.IconSwitch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureToggleListItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current

    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    ListItem(
        checked = checked,
        onCheckedChange = { newChecked ->
            onCheckedChange(newChecked)
            val feedback = if (newChecked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
            view.performHapticFeedback(feedback)
        },
        modifier = modifier
            .clip(RoundedCornerShape(64.dp))
            .height(72.dp),
        enabled = enabled,
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            selectedContainerColor = containerColor,
            contentColor = contentColor,
            selectedContentColor = contentColor,
            leadingContentColor = contentColor,
            selectedLeadingContentColor = contentColor,
            trailingContentColor = contentColor,
            selectedTrailingContentColor = contentColor
        ),
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        trailingContent = {
            IconSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        }
    )
}