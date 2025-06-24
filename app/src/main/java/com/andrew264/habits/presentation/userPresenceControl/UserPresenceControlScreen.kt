package com.andrew264.habits.presentation.userPresenceControl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.service.UserPresenceService
import com.andrew264.habits.state.UserPresenceState
import com.andrew264.habits.ui.theme.HabitsTheme

@Composable
fun UserPresenceControlScreen(
    modifier: Modifier = Modifier,
    viewModel: UserPresenceControlViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val presenceState by UserPresenceService.userPresenceState.collectAsState()
    val isServiceActive by UserPresenceService.isServiceActive.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Current User State:",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = presenceState.name.replace('_', ' '),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = when (presenceState) {
                UserPresenceState.AWAKE -> MaterialTheme.colorScheme.primary
                UserPresenceState.SLEEPING -> MaterialTheme.colorScheme.secondary
                UserPresenceState.WINDING_DOWN -> MaterialTheme.colorScheme.tertiary
                UserPresenceState.UNKNOWN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Presence Monitoring Active", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = isServiceActive,
                onCheckedChange = { isOn ->
                    if (isOn) {
                        viewModel.onStartService()
                    } else {
                        viewModel.onStopService()
                    }
                }
            )
        }

        Text(
            text = "The service uses the Sleep API.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Left,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Request Permissions")
        }

        Button(
            onClick = onOpenAppSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open App Settings")
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun UserPresenceControlScreenPreview() {
    HabitsTheme {
        UserPresenceControlScreen(
            onRequestPermissions = {},
            onOpenAppSettings = {}
        )
    }
}