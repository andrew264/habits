package com.andrew264.habits.ui.common.components.list

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationListItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: ListItemPosition = ListItemPosition.SEPARATE,
    valueContent: @Composable (RowScope.() -> Unit)? = null
) {
    val view = LocalView.current

    val trailingNode: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    }

    val clickAction: () -> Unit = {
        onClick()
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    Column(modifier = modifier) {
        if (position == ListItemPosition.SEPARATE) {
            ListItem(
                onClick = clickAction,
                enabled = enabled,
                shapes = position.toShapes(),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingLarge),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = icon,
                content = { Text(title) },
                trailingContent = trailingNode
            )
        } else {
            SegmentedListItem(
                onClick = clickAction,
                enabled = enabled,
                shapes = position.toShapes(),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingLarge),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = icon,
                content = { Text(title) },
                trailingContent = trailingNode
            )
        }
        if (position == ListItemPosition.TOP || position == ListItemPosition.MIDDLE) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                thickness = 2.dp
            )
        }
    }
}