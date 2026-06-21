package com.andrew264.habits.ui.common.components.list

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectionListItem(
    title: String,
    selectedValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: ListItemPosition = ListItemPosition.SEPARATE,
) {
    val view = LocalView.current
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
                content = { Text(title) },
                supportingContent = { Text(selectedValue) }
            )
        } else {
            SegmentedListItem(
                onClick = clickAction,
                enabled = enabled,
                shapes = position.toShapes(),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingLarge),
                verticalAlignment = Alignment.CenterVertically,
                content = { Text(title) },
                supportingContent = { Text(selectedValue) }
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