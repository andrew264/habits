package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.abs

@Composable
fun NumberPicker(
    modifier: Modifier = Modifier,
    items: List<String>,
    state: LazyListState,
    itemHeight: Dp,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    loop: Boolean = true,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }


    fun getCenterItemIndex(): Int {
        val layoutInfo = state.layoutInfo
        if (layoutInfo.visibleItemsInfo.isEmpty()) return -1

        val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportSize.height / 2)

        return layoutInfo.visibleItemsInfo
            .minByOrNull { itemInfo ->
                val itemCenter = itemInfo.offset + (itemInfo.size / 2)
                abs(itemCenter - viewportCenter)
            }?.index ?: -1
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .map { getCenterItemIndex() }
            .distinctUntilChanged()
            .filter { it != -1 }
            .collect { index ->
                val finalIndex = if (loop) index % items.size else index.coerceIn(items.indices)
                onValueChange(items[finalIndex])
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
            }
    }

    BoxWithConstraints(modifier = modifier) {
        val pickerHeight = this.maxHeight
        val topPadding = (pickerHeight - itemHeight) / 2

        val itemCount = if (loop) Int.MAX_VALUE else items.size

        val flingBehavior = run {
            val snapLayoutInfoProvider = remember(state) {
                SnapLayoutInfoProvider(lazyListState = state)
            }
            val highFrictionDecay: DecayAnimationSpec<Float> = remember {
                exponentialDecay(frictionMultiplier = 20f)
            }
            remember(snapLayoutInfoProvider, highFrictionDecay) {
                snapFlingBehavior(
                    snapLayoutInfoProvider = snapLayoutInfoProvider,
                    decayAnimationSpec = highFrictionDecay,
                    snapAnimationSpec = spring(stiffness = Spring.StiffnessMedium)
                )
            }
        }

        Box(contentAlignment = Alignment.Center) {
            LazyColumn(
                state = state,
                modifier = Modifier.height(pickerHeight),
                contentPadding = PaddingValues(top = topPadding, bottom = topPadding),
                flingBehavior = flingBehavior,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(count = itemCount, key = { it }) { index ->
                    val itemIndex = if (loop) index % items.size else index

                    val isSelected by remember {
                        derivedStateOf {
                            val layoutInfo = state.layoutInfo
                            val visibleItemsInfo = layoutInfo.visibleItemsInfo
                            if (visibleItemsInfo.isEmpty()) {
                                false
                            } else {
                                val itemInfo = visibleItemsInfo.find { it.index == index }
                                if (itemInfo != null) {
                                    val viewportCenter =
                                        layoutInfo.viewportStartOffset + (layoutInfo.viewportSize.height / 2)
                                    val itemCenter = itemInfo.offset + (itemInfo.size / 2)
                                    val distanceFromCenter = abs(itemCenter - viewportCenter)
                                    distanceFromCenter < itemHeightPx / 2f
                                } else {
                                    false
                                }
                            }
                        }
                    }

                    Text(
                        text = items[itemIndex],
                        style = textStyle,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(itemHeight)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .pickerItemAlpha(state, index, itemHeightPx)
                    )
                }
            }


            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = topPadding),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -topPadding),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

private fun Modifier.pickerItemAlpha(state: LazyListState, index: Int, itemHeightPx: Float): Modifier =
    this.graphicsLayer {
        val layoutInfo = state.layoutInfo
        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

        alpha = if (itemInfo != null) {
            val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportSize.height / 2)
            val itemCenter = itemInfo.offset + (itemInfo.size / 2)
            val distanceFromCenter = abs(itemCenter - viewportCenter)
            val maxDistance = itemHeightPx * 1.5f
            (1f - (distanceFromCenter / maxDistance)).coerceIn(0.3f, 1f)
        } else {
            0.3f
        }
    }

@Preview(showBackground = true)
@Composable
private fun NumberPickerPreview() {
    val items = (0..59).map { it.toString().padStart(2, '0') }
    val state = rememberLazyListState(Int.MAX_VALUE / 2)

    NumberPicker(
        modifier = Modifier.height(150.dp),
        items = items,
        state = state,
        itemHeight = 48.dp,
        onValueChange = {},
        loop = true
    )
}