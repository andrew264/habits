package com.andrew264.habits.ui.common.components.list

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.components.IconSwitch
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleListItem(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    position: ListItemPosition = ListItemPosition.MIDDLE,
    isWarningVisible: Boolean = false,
    warningText: String? = null,
    onWarningClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val performHapticToggle: (Boolean) -> Unit = { isChecked ->
        val feedback = if (isChecked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
        view.performHapticFeedback(feedback)
    }

    val leadingNode: @Composable () -> Unit = {
        Icon(imageVector = icon, contentDescription = null)
    }

    val contentNode: @Composable () -> Unit = {
        Text(title)
    }

    val supportingNode: @Composable (() -> Unit)? = if (summary.isNotEmpty() || (isWarningVisible && warningText != null)) {
        {
            Column {
                if (summary.isNotEmpty()) {
                    Text(summary)
                }
                if (isWarningVisible && warningText != null) {
                    Spacer(Modifier.height(Dimens.PaddingSmall))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onWarningClick != null, onClick = onWarningClick ?: {})
                            .padding(vertical = Dimens.PaddingExtraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = stringResource(id = R.string.toggle_list_item_warning),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    } else null

    val trailingNodeClickableRow: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.toggle_list_item_more_options),
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
            VerticalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = Dimens.PaddingMedium),
                color = if (enabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
            IconSwitch(
                checked = checked,
                onCheckedChange = { newChecked ->
                    onCheckedChange(newChecked)
                    performHapticToggle(newChecked)
                },
                enabled = enabled
            )
        }
    }

    val trailingNodeToggleRow: @Composable () -> Unit = {
        IconSwitch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }

    val listPadding = PaddingValues(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingLarge)
    val listColors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        selectedContainerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        selectedContentColor = MaterialTheme.colorScheme.onSurface,
        leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedLeadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedTrailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(modifier = modifier) {
        if (onClick != null) {
            val clickAction: () -> Unit = {
                onClick()
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            if (position == ListItemPosition.SEPARATE) {
                ListItem(
                    onClick = clickAction,
                    enabled = enabled,
                    shapes = position.toShapes(),
                    colors = listColors,
                    contentPadding = listPadding,
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = leadingNode,
                    content = contentNode,
                    supportingContent = supportingNode,
                    trailingContent = trailingNodeClickableRow
                )
            } else {
                SegmentedListItem(
                    onClick = clickAction,
                    enabled = enabled,
                    shapes = position.toShapes(),
                    colors = listColors,
                    contentPadding = listPadding,
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = leadingNode,
                    content = contentNode,
                    supportingContent = supportingNode,
                    trailingContent = trailingNodeClickableRow
                )
            }
        } else {
            val toggleAction = { newChecked: Boolean ->
                onCheckedChange(newChecked)
                performHapticToggle(newChecked)
            }
            if (position == ListItemPosition.SEPARATE) {
                ListItem(
                    checked = checked,
                    onCheckedChange = toggleAction,
                    enabled = enabled,
                    shapes = position.toShapes(),
                    colors = listColors,
                    contentPadding = listPadding,
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = leadingNode,
                    content = contentNode,
                    supportingContent = supportingNode,
                    trailingContent = trailingNodeToggleRow
                )
            } else {
                SegmentedListItem(
                    checked = checked,
                    onCheckedChange = toggleAction,
                    enabled = enabled,
                    shapes = position.toShapes(),
                    colors = listColors,
                    contentPadding = listPadding,
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = leadingNode,
                    content = contentNode,
                    supportingContent = supportingNode,
                    trailingContent = trailingNodeToggleRow
                )
            }
        }

        if (position == ListItemPosition.TOP || position == ListItemPosition.MIDDLE) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                thickness = 2.dp
            )
        }
    }
}