package com.andrew264.habits.ui.schedule.list

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrew264.habits.domain.analyzer.ScheduleAnalyzer
import com.andrew264.habits.model.schedule.Schedule
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
                }
            }
        }

        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            onDismiss = { direction ->
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onEdit()
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        val wasDeleted = onDelete()
                        if (!wasDeleted) {
                            dismissState.reset()
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
                            Spacer(Modifier.width(8.dp))
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = schedule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 20.sp,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
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

                        Spacer(Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}