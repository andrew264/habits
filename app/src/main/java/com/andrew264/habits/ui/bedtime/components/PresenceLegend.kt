package com.andrew264.habits.ui.bedtime.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun PresenceLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = UserPresenceState.AWAKE.toColor(), label = stringResource(id = R.string.presence_legend_awake))
        LegendItem(color = UserPresenceState.SLEEPING.toColor(), label = stringResource(id = R.string.presence_legend_sleeping))
        LegendItem(color = UserPresenceState.UNKNOWN.toColor(), label = stringResource(id = R.string.presence_legend_unknown))
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.PaddingMedium)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PresenceLegendPreview() {
    PresenceLegend()
}

@Preview(showBackground = true)
@Composable
fun LegendItemPreview() {
    LegendItem(
        color = UserPresenceState.AWAKE.toColor(),
        label = "Awake"
    )
}

fun UserPresenceState.toColor(): Color {
    return when (this) {
        UserPresenceState.AWAKE -> Color(0xFF4CAF50) // Green
        UserPresenceState.SLEEPING -> Color(0xFF3F51B5) // Indigo
        UserPresenceState.UNKNOWN -> Color(0xFF9E9E9E) // Grey
    }
}