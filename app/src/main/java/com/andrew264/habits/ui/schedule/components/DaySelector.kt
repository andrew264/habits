package com.andrew264.habits.ui.schedule.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.model.schedule.DayOfWeek
import com.andrew264.habits.ui.theme.Dimens
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onDayClick: (DayOfWeek) -> Unit,
) {
    val view = LocalView.current

    Column(
        modifier = Modifier.padding(Dimens.PaddingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
    ) {
        Text(
            text = "Days",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DayOfWeek.entries.forEach { day ->
                val selected = day in selectedDays
                val dayName = day.name.take(1).replaceFirstChar { it.titlecase(Locale.getDefault()) }

                val backgroundColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else Color.Transparent,
                    label = "DaySelectorBackgroundColor"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    label = "DaySelectorContentColor"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                    label = "DaySelectorBorderColor"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color = backgroundColor)
                        .border(width = 1.dp, color = borderColor, shape = CircleShape)
                        .clickable {
                            onDayClick(day)
                            val feedback = if (!selected) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                            view.performHapticFeedback(feedback)
                        }
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun DaySelectorPreview() {
    val selectedDays = remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)) }
    DaySelector(
        selectedDays = selectedDays.value,
        onDayClick = { day ->
            selectedDays.value = if (day in selectedDays.value) selectedDays.value - day else selectedDays.value + day
        })
}