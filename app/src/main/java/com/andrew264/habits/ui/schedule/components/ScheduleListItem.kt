package com.andrew264.habits.ui.schedule.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.domain.analyzer.ScheduleAnalyzer
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.model.schedule.ScheduleGroup
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ScheduleListItem(
    schedule: Schedule,
    isPendingDeletion: Boolean,
    onDelete: suspend () -> Boolean,
    onEdit: () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = !isPendingDeletion,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(
            animationSpec = tween(durationMillis = 250)
        )
    ) {
        val dismissState = rememberSwipeToDismissBoxState()

        LaunchedEffect(dismissState.targetValue) {
            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
            }
        }

        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.clip(MaterialTheme.shapes.medium),
            onDismiss = { direction ->
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        scope.launch {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onEdit()
                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        scope.launch {
                            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            val wasDeleted = onDelete()
                            if (!wasDeleted) {
                                dismissState.reset()
                            }
                        }
                    }

                    SwipeToDismissBoxValue.Settled -> { /* Do nothing */
                    }
                }
            },
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val color by animateColorAsState(
                    targetValue = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                    },
                    animationSpec = tween(300),
                    label = "SwipeBackgroundColor"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                ) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                            Spacer(Modifier.width(Dimens.PaddingSmall))
                            Text("Edit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("Delete", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(Dimens.PaddingSmall))
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        ) {
            val analyzer = remember(schedule.groups) { ScheduleAnalyzer(schedule.groups) }
            val summary = remember(analyzer) { analyzer.createSummary() }
            val coverage = remember(analyzer) { analyzer.calculateCoverage() }

            Card(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onEdit()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .animateContentSize()
                        .padding(horizontal = 20.dp, vertical = Dimens.PaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    Text(
                        text = schedule.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    val summaryText =
                        if (summary.isNotBlank() && summary != "No schedule set.") {
                            summary
                        } else {
                            "This schedule is empty. Edit to add times."
                        }
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "Total hours",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", coverage.totalHours)} hours/week (${
                                String.format(Locale.getDefault(), "%.1f", coverage.coveragePercentage)
                            }%)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.width(Dimens.PaddingLarge))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
internal fun ScheduleListItemPreview() {
    val schedule = Schedule(
        id = "1",
        name = "Work Schedule",
        groups = listOf(
            ScheduleGroup(
                id = "group1",
                name = "Weekdays",
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                timeRanges = listOf(
                    com.andrew264.habits.model.schedule.TimeRange(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 17 * 60) // 9 AM to 5 PM
                )
            )
        )
    )
    ScheduleListItem(
        schedule = schedule,
        isPendingDeletion = false,
        onDelete = { true },
        onEdit = {}
    )
}