package com.andrew264.habits.ui.common.list_items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
fun <T> ContainedLazyColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    onItemClick: ((item: T) -> Unit)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        containedItems(
            items = items,
            key = key,
            onItemClick = onItemClick,
            itemContent = itemContent
        )
    }
}

fun <T> LazyListScope.containedItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    onItemClick: ((item: T) -> Unit)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {
    itemsIndexed(items = items, key = if (key != null) { _, item -> key(item) } else null) { index, item ->
        val position = when {
            items.size == 1 -> ListItemPosition.SEPARATE
            index == 0 -> ListItemPosition.TOP
            index == items.lastIndex -> ListItemPosition.BOTTOM
            else -> ListItemPosition.MIDDLE
        }

        ListItemContainer(
            position = position,
            onClick = if (onItemClick != null) {
                { onItemClick(item) }
            } else {
                null
            }
        ) {
            itemContent(item)
        }
    }
}


@Preview(showBackground = false, widthDp = 300)
@Composable
private fun ContainedLazyColumnPreview() {
    val sampleItems = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig", "Grape")
    HabitsTheme {
        ContainedLazyColumn(
            items = sampleItems,
            onItemClick = {}
        ) { item ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingLarge)
            )
        }
    }
}

@Preview(showBackground = false, widthDp = 300)
@Composable
private fun ContainedLazyColumnSingleItemPreview() {
    val sampleItems = listOf("Single Item")
    HabitsTheme {
        ContainedLazyColumn(
            items = sampleItems,
            onItemClick = {}
        ) { item ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingLarge)
            )
        }
    }
}