package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens


enum class ListItemPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SEPARATE
}

@Composable
fun ToggleSettingsListItem(
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

    val groupCornerRadius = 20.dp
    val innerCornerRadius = 4.dp

    val clipShape = when (position) {
        ListItemPosition.SEPARATE -> RoundedCornerShape(groupCornerRadius)
        ListItemPosition.TOP -> RoundedCornerShape(topStart = groupCornerRadius, topEnd = groupCornerRadius, bottomStart = innerCornerRadius, bottomEnd = innerCornerRadius)
        ListItemPosition.MIDDLE -> RoundedCornerShape(innerCornerRadius)
        ListItemPosition.BOTTOM -> RoundedCornerShape(topStart = innerCornerRadius, topEnd = innerCornerRadius, bottomStart = groupCornerRadius, bottomEnd = groupCornerRadius)
    }

    val performHapticToggle: (Boolean) -> Unit = { isChecked ->
        val feedback = if (isChecked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
        view.performHapticFeedback(feedback)
    }

    Column(modifier = modifier) {
        Surface(
            shape = clipShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                enabled = enabled,
                                onClick = onClick ?: {
                                    val newChecked = !checked
                                    onCheckedChange(newChecked)
                                    performHapticToggle(newChecked)
                                }
                            )
                            .padding(Dimens.PaddingLarge),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraLarge)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Spacer(modifier = Modifier.height(Dimens.PaddingExtraSmall))
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        if (onClick != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "More options",
                                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                    }

                    if (onClick != null) {
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight(0.60f)
                                .padding(vertical = Dimens.PaddingSmall),
                            color = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = Dimens.PaddingMedium)
                            .wrapContentSize(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Switch(
                            checked = checked,
                            onCheckedChange = if (onClick != null) { newChecked ->
                                onCheckedChange(newChecked)
                                performHapticToggle(newChecked)
                            } else {
                                null
                            },
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    }
                }

                AnimatedVisibility(visible = isWarningVisible && warningText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onWarningClick != null, onClick = onWarningClick ?: {})
                            .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = warningText!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (position == ListItemPosition.TOP || position == ListItemPosition.MIDDLE) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest, thickness = 2.dp)
        }
    }
}

@Preview(heightDp = 400, showBackground = true)
@Composable
fun ToggleSettingsListItemPreview() {
    val checkedState = remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.padding(Dimens.PaddingMedium),
        verticalArrangement = Arrangement.Center
    ) {
        ToggleSettingsListItem(
            icon = Icons.Filled.Info,
            title = "Sample Toggle Item",
            summary = "This is a sample summary for the toggle item.",
            checked = checkedState.value,
            onCheckedChange = { checkedState.value = it },
            position = ListItemPosition.TOP
        )
        ToggleSettingsListItem(
            icon = Icons.Filled.Info,
            title = "Sample Toggle Item with Warning",
            summary = "This item has a warning associated with it.",
            checked = checkedState.value,
            onCheckedChange = { checkedState.value = it },
            onClick = {},
            isWarningVisible = true,
            warningText = "This is a warning message!",
            onWarningClick = {},
            position = ListItemPosition.BOTTOM
        )
    }
}

@Composable
fun NavigationSettingsListItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: ListItemPosition = ListItemPosition.SEPARATE,
    valueContent: @Composable (RowScope.() -> Unit)? = null
) {
    val view = LocalView.current

    val groupCornerRadius = 20.dp
    val innerCornerRadius = 4.dp

    val clipShape = when (position) {
        ListItemPosition.SEPARATE -> RoundedCornerShape(groupCornerRadius)
        ListItemPosition.TOP -> RoundedCornerShape(topStart = groupCornerRadius, topEnd = groupCornerRadius, bottomStart = innerCornerRadius, bottomEnd = innerCornerRadius)
        ListItemPosition.MIDDLE -> RoundedCornerShape(innerCornerRadius)
        ListItemPosition.BOTTOM -> RoundedCornerShape(topStart = innerCornerRadius, topEnd = innerCornerRadius, bottomStart = groupCornerRadius, bottomEnd = groupCornerRadius)
    }

    Column(modifier = modifier) {
        Surface(
            shape = clipShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        enabled = enabled,
                        onClick = {
                            onClick()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    )
                    .padding(horizontal = Dimens.PaddingLarge, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
                Spacer(modifier = Modifier.width(Dimens.PaddingLarge))
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    if (valueContent != null) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.bodyMedium.copy(
                                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        ) {
                            valueContent()
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }
        if (position == ListItemPosition.TOP || position == ListItemPosition.MIDDLE) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest, thickness = 2.dp)
        }
    }
}

@Preview
@Composable
fun NavigationSettingsListItemPreview() {
    NavigationSettingsListItem(
        icon = Icons.Filled.Info,
        title = "Sample Navigation Item",
        onClick = {},
        position = ListItemPosition.SEPARATE,
        valueContent = {
            Text("Value", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}
