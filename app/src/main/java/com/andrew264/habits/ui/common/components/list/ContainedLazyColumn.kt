package com.andrew264.habits.ui.common.components.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A helper for rendering lists that need to know the positional shape of their items.
 * Instead of wrapping the items in a hidden container, it passes the [ListItemPosition]
 * directly to the content block so the UI components can apply [ListItemDefaults.segmentedShapes].
 */
@Composable
fun <T> ContainedLazyColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable (item: T, position: ListItemPosition) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        containedItems(
            items = items,
            key = key,
            itemContent = itemContent
        )
    }
}

fun <T> LazyListScope.containedItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable (item: T, position: ListItemPosition) -> Unit,
) {
    itemsIndexed(items = items, key = if (key != null) { _, item -> key(item) } else null) { index, item ->
        val position = when {
            items.size == 1 -> ListItemPosition.SEPARATE
            index == 0 -> ListItemPosition.TOP
            index == items.lastIndex -> ListItemPosition.BOTTOM
            else -> ListItemPosition.MIDDLE
        }
        itemContent(item, position)
    }
}