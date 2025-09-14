package com.andrew264.habits.ui.common.list_items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun SelectionListItem(
    title: String,
    selectedValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: ListItemPosition = ListItemPosition.SEPARATE,
) {
    ListItemContainer(
        position = position,
        modifier = modifier,
        onClick = if (enabled) onClick else null,
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingLarge)
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
}

@Preview
@Composable
fun SelectionListItemPreview() {
    SelectionListItem(
        title = "Sample Title",
        selectedValue = "Sample Value",
        onClick = {}
    )
}