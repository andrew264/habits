package com.andrew264.habits.ui.bedtime.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.domain.analyzer.ScheduleCoverage
import com.andrew264.habits.ui.bedtime.ScheduleInfo
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
fun ScheduleInfoCard(
    scheduleInfo: ScheduleInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text(
                text = scheduleInfo.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Dimens.PaddingMedium))
            HorizontalDivider()
            Spacer(Modifier.height(Dimens.PaddingMedium))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = stringResource(id = R.string.schedule_info_card_total_hours_content_description),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        id = R.string.schedule_info_card_coverage_text,
                        scheduleInfo.coverage.totalHours,
                        scheduleInfo.coverage.coveragePercentage
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun ScheduleInfoCardPreview() {
    HabitsTheme {
        ScheduleInfoCard(
            scheduleInfo = ScheduleInfo(
                summary = "Mon-Fri: 11:00 PM - 7:00 AM (+1d)\nSat, Sun: 12:00 AM - 9:00 AM (+1d)",
                coverage = ScheduleCoverage(totalHours = 58.0, coveragePercentage = 34.5)
            )
        )
    }
}