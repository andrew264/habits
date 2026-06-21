package com.andrew264.habits.ui.counters.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.domain.usecase.counter.CounterWithProgress
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.components.list.ListItemPosition
import com.andrew264.habits.ui.common.components.list.toShapes
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CounterListItem(
    item: CounterWithProgress,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val clickAction: () -> Unit = {
        onClick()
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    val counterColor = item.counter.colorHex.toColorOrNull() ?: MaterialTheme.colorScheme.primary
    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)

    val leadingNode: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(counterColor)
        )
    }

    val contentNode: @Composable () -> Unit = {
        Text(
            text = item.counter.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    val trailingNode: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
        ) {
            val valueText = FormatUtils.formatCounterValue(item.todayValue, item.counter.type)
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.hasLogsToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val supportingNode: @Composable () -> Unit = {
        if (item.counter.target != null && item.counter.target > 0) {
            Column {
                Spacer(Modifier.height(Dimens.PaddingSmall))
                val progress = (item.todayValue / item.counter.target).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = counterColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Spacer(Modifier.height(Dimens.PaddingExtraSmall))
                Text(
                    text = stringResource(id = R.string.counters_list_item_target, FormatUtils.formatCounterValue(item.counter.target, item.counter.type)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (position == ListItemPosition.SEPARATE) {
        ListItem(
            onClick = clickAction,
            modifier = modifier,
            shapes = position.toShapes(),
            colors = colors,
            leadingContent = leadingNode,
            content = contentNode,
            trailingContent = trailingNode,
            supportingContent = if (item.counter.target != null && item.counter.target > 0) supportingNode else null
        )
    } else {
        SegmentedListItem(
            onClick = clickAction,
            modifier = modifier,
            shapes = position.toShapes(),
            colors = colors,
            leadingContent = leadingNode,
            content = contentNode,
            trailingContent = trailingNode,
            supportingContent = if (item.counter.target != null && item.counter.target > 0) supportingNode else null
        )
    }
}
