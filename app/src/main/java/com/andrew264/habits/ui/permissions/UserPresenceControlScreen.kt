package com.andrew264.habits.ui.permissions

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserPresenceControlScreen(
    modifier: Modifier = Modifier,
    viewModel: UserPresenceControlViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        Text(
            text = "Current User State:",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = uiState.presenceState.name.replace('_', ' '),
            style = MaterialTheme.typography.headlineMedium,
            color = when (uiState.presenceState) {
                UserPresenceState.AWAKE -> MaterialTheme.colorScheme.primary
                UserPresenceState.SLEEPING -> MaterialTheme.colorScheme.secondary
                UserPresenceState.UNKNOWN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Presence Monitoring Active", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = uiState.isServiceActive,
                onCheckedChange = { isOn ->
                    if (isOn) {
                        viewModel.onStartService()
                    } else {
                        viewModel.onStopService()
                    }
                    val feedback = if (isOn) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                    view.performHapticFeedback(feedback)
                }
            )
        }

        Text(
            text = "The service uses the Sleep API and time-based heuristics.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Left,
            modifier = Modifier.padding(horizontal = Dimens.PaddingExtraSmall)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall))

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