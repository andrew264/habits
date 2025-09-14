package com.andrew264.habits.ui.common.list_items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect

enum class ListItemPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SEPARATE
}

@Composable
fun ListItemContainer(
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val groupCornerRadius = 20.dp
    val innerCornerRadius = 4.dp

    val clipShape = when (position) {
        ListItemPosition.SEPARATE -> RoundedCornerShape(groupCornerRadius)
        ListItemPosition.TOP -> RoundedCornerShape(topStart = groupCornerRadius, topEnd = groupCornerRadius, bottomStart = innerCornerRadius, bottomEnd = innerCornerRadius)
        ListItemPosition.MIDDLE -> RoundedCornerShape(innerCornerRadius)
        ListItemPosition.BOTTOM -> RoundedCornerShape(topStart = innerCornerRadius, topEnd = innerCornerRadius, bottomStart = groupCornerRadius, bottomEnd = groupCornerRadius)
    }

    val interactionSource = remember { MutableInteractionSource() }
    HapticInteractionEffect(interactionSource)

    Column(modifier = modifier) {
        Surface(
            shape = clipShape,
            color = containerColor,
            modifier = Modifier.then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
        ) {
            content()
        }
        if (position == ListItemPosition.TOP || position == ListItemPosition.MIDDLE) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest, thickness = 2.dp)
        }
    }
}