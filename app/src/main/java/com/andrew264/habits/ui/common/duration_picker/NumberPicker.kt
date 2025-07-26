package com.andrew264.habits.ui.common.duration_picker

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

@Composable
fun NumberPicker(
    modifier: Modifier = Modifier,
    items: List<String>,
    selectedItem: String,
    onValueChange: (String) -> Unit,
    loop: Boolean = false
) {
    val itemHeight = 48.dp
    val pickerHeight = itemHeight * 3

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val view = LocalView.current

    // Scroll to the initial selected item
    LaunchedEffect(listState, items, selectedItem) {
        val initialIndex = items.indexOf(selectedItem)
        if (initialIndex != -1) {
            val centerIndex = if (loop) {
                (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % items.size) + initialIndex
            } else {
                initialIndex
            }
            listState.scrollToItem(centerIndex)
        }
    }

    // Find the index of the item closest to the center of the viewport
    val centralItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                -1
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                layoutInfo.visibleItemsInfo
                    .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: -1
            }
        }
    }

    // Get the actual selected index in the original items list
    val selectedIndex = if (centralItemIndex != -1) {
        if (loop) centralItemIndex % items.size else centralItemIndex
    } else {
        -1
    }

    // Vibrate when the central item changes
    LaunchedEffect(listState) {
        snapshotFlow { centralItemIndex }
            .drop(1) // Ignore initial composition
            .distinctUntilChanged()
            .collect {
                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
            }
    }

    // Report value change when scrolling stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && selectedIndex != -1) {
            val finalItem = items[selectedIndex]
            if (finalItem != selectedItem) {
                onValueChange(finalItem)
            }
        }
    }

    Box(
        modifier = modifier.height(pickerHeight),
        contentAlignment = Alignment.Center
    ) {
        // The list of numbers that scrolls
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = itemHeight), // Pads top and bottom for centering
            modifier = Modifier.fillMaxWidth()
        ) {
            val count = if (loop) Int.MAX_VALUE else items.size
            items(count) { index ->
                val itemIndex = if (loop) index % items.size else index
                val isSelected = index == centralItemIndex
                Text(
                    text = items[itemIndex],
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .height(itemHeight)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
            }
        }

        // The static horizontal lines overlay
        val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        val dividerModifier = Modifier
            .fillMaxWidth(0.6f)
            .align(Alignment.Center)
        HorizontalDivider(
            modifier = dividerModifier.offset(y = -itemHeight / 2),
            thickness = 2.dp,
            color = dividerColor
        )
        HorizontalDivider(
            modifier = dividerModifier.offset(y = itemHeight / 2),
            thickness = 2.dp,
            color = dividerColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NumberPickerPreview() {
    MaterialTheme {
        NumberPicker(
            items = (0..59).map { it.toString() },
            selectedItem = "30",
            onValueChange = {},
            loop = true
        )
    }
}