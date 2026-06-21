package com.andrew264.habits.ui.common.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun InfoListItem(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    position: ListItemPosition = ListItemPosition.SEPARATE
) {
    Column(modifier = modifier) {
        Surface(
            shape = position.toStaticShape(),
            color = MaterialTheme.colorScheme.surface
        ) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(text) },
                leadingContent = { Icon(imageVector = icon, contentDescription = null) },
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