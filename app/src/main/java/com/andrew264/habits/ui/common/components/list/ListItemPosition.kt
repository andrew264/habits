package com.andrew264.habits.ui.common.components.list

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class ListItemPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SEPARATE
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListItemPosition.toShapes(): ListItemShapes {
    val shape = this.toStaticShape()
    return ListItemDefaults.shapes(
        shape = shape,
        selectedShape = shape,
        pressedShape = shape,
        focusedShape = shape,
        hoveredShape = shape,
        draggedShape = shape
    )
}

/**
 * Returns a static Shape for non-interactive containers (like InfoListItem) that cannot
 * accept the state-driven ListItemShapes object.
 */
@Composable
fun ListItemPosition.toStaticShape(): Shape {
    val outer = 16.dp
    val inner = 4.dp

    return when (this) {
        ListItemPosition.TOP -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        ListItemPosition.MIDDLE -> RoundedCornerShape(inner)
        ListItemPosition.BOTTOM -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        ListItemPosition.SEPARATE -> RoundedCornerShape(outer)
    }
}
