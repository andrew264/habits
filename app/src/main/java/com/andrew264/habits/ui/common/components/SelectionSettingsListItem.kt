package com.andrew264.habits.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun SelectionSettingsListItem(
    title: String,
    selectedValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: ListItemPosition = ListItemPosition.SEPARATE,
) {
    val interactionSource = remember { MutableInteractionSource() }
    HapticInteractionEffect(interactionSource)
    val padValue = Dimens.PaddingLarge
    val clipShape = when (position) {
        ListItemPosition.TOP -> RoundedCornerShape(topStart = padValue, topEnd = padValue)
        ListItemPosition.MIDDLE -> RectangleShape
        ListItemPosition.BOTTOM -> RoundedCornerShape(bottomStart = padValue, bottomEnd = padValue)
        ListItemPosition.SEPARATE -> RoundedCornerShape(padValue)
    }

    Column(modifier = modifier) {
        Surface(
            shape = clipShape,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingLarge)
            ) {
                val contentAlpha = if (enabled) LocalContentColor.current.alpha else 0.38f
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = selectedValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
        }
        if (position == ListItemPosition.TOP || position == ListItemPosition.MIDDLE) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, thickness = 2.dp)
        }
    }
}

@Preview
@Composable
fun SelectionSettingsListItemPreview() {
    SelectionSettingsListItem(
        title = "Sample Title",
        selectedValue = "Sample Value",
        onClick = {}
    )
}


@Composable
fun InfoListItem(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    position: ListItemPosition = ListItemPosition.SEPARATE
) {
    val padValue = Dimens.PaddingLarge
    val clipShape = when (position) {
        ListItemPosition.TOP -> RoundedCornerShape(topStart = padValue, topEnd = padValue)
        ListItemPosition.MIDDLE -> RectangleShape
        ListItemPosition.BOTTOM -> RoundedCornerShape(bottomStart = padValue, bottomEnd = padValue)
        ListItemPosition.SEPARATE -> RoundedCornerShape(padValue)
    }

    Surface(
        shape = clipShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun InfoListItemPreview() {
    InfoListItem(
        text = "Sample information text.",
        icon = Icons.Filled.Info
    )
}

