package com.andrew264.habits.ui.counters.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.domain.usecase.counter.CounterWithProgress
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun CounterListItem(
    item: CounterWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    HapticInteractionEffect(interactionSource)

    val counterColor = item.counter.colorHex.toColorOrNull() ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(counterColor)
                    )
                    Text(
                        text = item.counter.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val valueText = FormatUtils.formatCounterValue(item.todayValue, item.counter.type)
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (item.hasLogsToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }

            if (item.counter.target != null && item.counter.target > 0) {
                Spacer(Modifier.height(Dimens.PaddingMedium))
                val progress = (item.todayValue / item.counter.target).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = counterColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(Dimens.PaddingExtraSmall))
                Text(
                    text = stringResource(id = R.string.counters_list_item_target, FormatUtils.formatCounterValue(item.counter.target, item.counter.type)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}