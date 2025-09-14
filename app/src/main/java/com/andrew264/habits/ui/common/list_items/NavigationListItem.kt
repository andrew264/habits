package com.andrew264.habits.ui.common.list_items

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun NavigationListItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: ListItemPosition = ListItemPosition.SEPARATE,
    valueContent: @Composable (RowScope.() -> Unit)? = null
) {
    val view = LocalView.current

    ListItemContainer(
        position = position,
        modifier = modifier
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
}

@Preview
@Composable
fun NavigationListItemPreview() {
    NavigationListItem(
        icon = Icons.Filled.Info,
        title = "Sample Navigation Item",
        onClick = {},
        position = ListItemPosition.SEPARATE,
        valueContent = {
            Text("Value", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}