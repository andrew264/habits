package com.andrew264.habits.ui.counters.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrew264.habits.domain.model.CounterLog
import com.andrew264.habits.model.counter.CounterType
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.Dimens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface TimelineItem {
    val key: String

    data class LogEntry(
        val log: CounterLog,
        val isTop: Boolean,
        val isBottom: Boolean
    ) : TimelineItem {
        override val key = "log_${log.id}"
    }

    data class TimeSkip(val id: String) : TimelineItem {
        override val key = "skip_$id"
    }
}

@Composable
fun rememberTimelineItems(logs: List<CounterLog>): List<TimelineItem> {
    return remember(logs) {
        val list = mutableListOf<TimelineItem>()
        for (i in logs.indices) {
            val log = logs[i]
            val prevLog = logs.getOrNull(i - 1)
            val nextLog = logs.getOrNull(i + 1)

            val gapAbove = prevLog != null && (prevLog.timestamp - log.timestamp) >= 30 * 60 * 1000L
            val gapBelow = nextLog != null && (log.timestamp - nextLog.timestamp) >= 30 * 60 * 1000L

            val isTop = prevLog == null || gapAbove
            val isBottom = nextLog == null || gapBelow

            list.add(TimelineItem.LogEntry(log, isTop, isBottom))

            if (gapBelow) {
                list.add(TimelineItem.TimeSkip(log.id.toString()))
            }
        }
        list
    }
}

@Composable
fun TimelineLogItem(
    entry: TimelineItem.LogEntry,
    counterType: CounterType,
    color: Color,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date = Instant.ofEpochMilli(entry.log.timestamp).atZone(ZoneId.systemDefault())
    val timeString = date.format(DateTimeFormatter.ofPattern("h:mm a"))
    val valueString = FormatUtils.formatCounterValue(entry.log.value, counterType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = Dimens.PaddingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        ) {
            val strokeWidth = 4.dp.toPx()
            val anchorY = size.height / 2

            val startY = if (entry.isTop) anchorY else -50f
            val endY = if (entry.isBottom) anchorY else size.height + 50f

            drawLine(
                color = color,
                start = Offset(size.width / 2, startY),
                end = Offset(size.width / 2, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Dimens.PaddingMedium, horizontal = Dimens.PaddingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(valueString, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(timeString, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete log",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TimelineSkipItem(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge)
    ) {
        Canvas(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
        ) {
            val radius = 2.5.dp.toPx()
            val x = size.width / 2
            val spacing = size.height / 4
            drawCircle(color, radius, Offset(x, spacing))
            drawCircle(color, radius, Offset(x, spacing * 2))
            drawCircle(color, radius, Offset(x, spacing * 3))
        }
    }
}