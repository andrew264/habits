package com.andrew264.habits.ui.usage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.usage.AppDetails
import java.util.Locale

@Composable
fun AppListItem(
    appDetails: AppDetails,
    onColorSwatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        DrawableImage(
            drawable = appDetails.icon,
            contentDescription = "${appDetails.friendlyName} icon",
            modifier = Modifier.size(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appDetails.friendlyName,
                style = MaterialTheme.typography.bodyLarge,
            )
            val sessionText = if (appDetails.sessionCount == 1) "1 session" else "${appDetails.sessionCount} sessions"
            val percentageText = String.format(Locale.getDefault(), "%.1f%%", appDetails.usagePercentage * 100)
            Text(
                text = "$percentageText • $sessionText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatDuration(appDetails.totalUsageMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(appDetails.color.toColorInt()))
                .clickable(onClick = onColorSwatchClick)
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0m"
    val totalMinutes = millis / 1000 / 60
    if (totalMinutes < 1) return "<1m"
    if (totalMinutes < 60) return "${totalMinutes}m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
}

@Preview(showBackground = true)
@Composable
private fun AppListItemPreview() {
    val sampleAppDetails = AppDetails(
        packageName = "com.example.app",
        friendlyName = "Sample Application",
        icon = null, // Drawables are hard to preview
        color = "#4CAF50",
        totalUsageMillis = (3600000L * 2) + (60000L * 33), // 2h 33m
        usagePercentage = 0.35f,
        sessionCount = 12
    )
    HabitsTheme {
        AppListItem(
            appDetails = sampleAppDetails,
            onColorSwatchClick = {},
            modifier = Modifier.padding(Dimens.PaddingLarge)
        )
    }
}