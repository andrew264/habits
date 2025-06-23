package com.andrew264.habits.presentation.userPresenceControl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.service.UserPresenceService
import com.andrew264.habits.state.UserPresenceState
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserPresenceControlScreen(
    modifier: Modifier = Modifier,
    viewModel: UserPresenceControlViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val presenceState by UserPresenceService.userPresenceState.collectAsState()
    val currentOperatingMode by UserPresenceService.currentOperatingMode.collectAsState()

    var preferredModeOnStart by remember { mutableStateOf(UserPresenceService.OperatingMode.HEURISTICS_ACTIVE) }

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
            text = presenceState.name,
            style = MaterialTheme.typography.headlineSmall,
            color = when (presenceState) {
                UserPresenceState.AWAKE -> MaterialTheme.colorScheme.primary
                UserPresenceState.SLEEPING -> MaterialTheme.colorScheme.secondary
                UserPresenceState.UNKNOWN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Select Monitoring Mode:", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier.fillMaxWidth()
        ) {
            ToggleButton(
                checked = currentOperatingMode == UserPresenceService.OperatingMode.SLEEP_API_ACTIVE,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        viewModel.onStartWithSleepApi()
                        preferredModeOnStart = UserPresenceService.OperatingMode.SLEEP_API_ACTIVE
                    }
                },
                modifier = Modifier.weight(1f),
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
            ) {
                Text("Sleep API")
            }
            ToggleButton(
                checked = currentOperatingMode == UserPresenceService.OperatingMode.HEURISTICS_ACTIVE,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        viewModel.onStartWithHeuristics()
                        preferredModeOnStart = UserPresenceService.OperatingMode.HEURISTICS_ACTIVE
                    }
                },
                modifier = Modifier.weight(1f),
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
            ) {
                Text("Heuristics")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Service Active:", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = currentOperatingMode != UserPresenceService.OperatingMode.STOPPED,
                onCheckedChange = { isOn ->
                    if (isOn) {
                        if (preferredModeOnStart == UserPresenceService.OperatingMode.SLEEP_API_ACTIVE) {
                            viewModel.onStartWithSleepApi()
                        } else {
                            viewModel.onStartWithHeuristics()
                        }
                    } else {
                        viewModel.onStopService()
                    }
                }
            )
        }

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