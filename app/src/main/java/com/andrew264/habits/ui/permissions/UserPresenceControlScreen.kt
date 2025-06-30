package com.andrew264.habits.ui.permissions

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.service.UserPresenceService
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserPresenceControlScreen(
    modifier: Modifier = Modifier,
    viewModel: UserPresenceControlViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val presenceState by UserPresenceService.userPresenceState.collectAsState()
    val isServiceActive by UserPresenceService.isServiceActive.collectAsState()
    val view = LocalView.current

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val feedback = if (isOn) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                        view.performHapticFeedback(feedback)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                }
            )
        }

        Text(
            text = "The service uses the Sleep API and time-based heuristics.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Left,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Button(
            onClick = {
                onRequestPermissions()
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            },
            modifier = Modifier.fillMaxWidth(),
            shapes = ButtonDefaults.shapes()
        ) {
            Text("Request Permissions")
        }

        Button(
            onClick = {
                onOpenAppSettings()
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            },
            modifier = Modifier.fillMaxWidth(),
            shapes = ButtonDefaults.shapes()
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