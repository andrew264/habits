package com.andrew264.habits.ui.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

/**
 * A simple data class to represent a single statistic.
 */
data class Statistic(
    val label: String,
    val value: String
)

/**
 * A card that displays a list of statistics in a flow layout,
 * wrapping items to the next row when they don't fit.
 *
 * @param statistics A list of [Statistic] objects to display.
 * @param modifier The modifier to be applied to the card.
 * @param minItemWidth The minimum width for each statistic item, helping control wrapping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticCard(
    statistics: List<Statistic>,
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 120.dp
) {
    Card(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.PaddingLarge, horizontal = Dimens.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge),
            maxItemsInEachRow = statistics.size
        ) {
            statistics.forEach { statistic ->
                StatisticItem(
                    label = statistic.label,
                    value = statistic.value,
                    modifier = Modifier
                        .widthIn(min = minItemWidth)
                        .padding(horizontal = Dimens.PaddingSmall)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatisticCardPreview_3Items() {
    val sampleStats = listOf(
        Statistic("Screen Time", "4h 32m"),
        Statistic("Unlocks", "58"),
        Statistic("Avg. Session", "4m 41s")
    )
    HabitsTheme {
        StatisticCard(statistics = sampleStats, modifier = Modifier.padding(Dimens.PaddingLarge))
    }
}

@Preview(showBackground = true, widthDp = 300)
@Composable
private fun StatisticCardPreview_2Items_Narrow() {
    val sampleStats = listOf(
        Statistic("Daily Avg", "2150 ml"),
        Statistic("Goal Met", "4 days")
    )
    HabitsTheme {
        StatisticCard(statistics = sampleStats, modifier = Modifier.padding(Dimens.PaddingLarge))
    }
}

@Preview(showBackground = true)
@Composable
private fun StatisticCardPreview_5Items() {
    val sampleStats = listOf(
        Statistic("Stat 1", "Value 1"),
        Statistic("Stat 2", "Value 2"),
        Statistic("Stat 3", "Value 3"),
        Statistic("Stat 4", "Value 4"),
        Statistic("Stat 5", "Value 5")
    )
    HabitsTheme {
        StatisticCard(statistics = sampleStats, modifier = Modifier.padding(Dimens.PaddingLarge))
    }
}