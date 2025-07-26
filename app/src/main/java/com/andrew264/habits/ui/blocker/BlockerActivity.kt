package com.andrew264.habits.ui.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.andrew264.habits.MainActivity
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.navigation.Usage
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockerActivity : ComponentActivity() {

    private val viewModel: BlockerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(viewModel.events) {
                viewModel.events.collect { event ->
                    when (event) {
                        is BlockerEvent.Finish -> finish()
                        is BlockerEvent.NavigateToHome -> {
                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                            finish()
                        }

                        is BlockerEvent.NavigateToAppDetails -> {
                            val intent = Intent(this@BlockerActivity, MainActivity::class.java).apply {
                                putExtra("destination_route", Usage::class.java.simpleName + "/" + event.packageName)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }

            HabitsTheme {
                uiState?.let {
                    BlockerScreen(
                        uiState = it,
                        onSnoozeClicked = viewModel::onSnoozeClicked,
                        onImDoneClicked = viewModel::onImDoneClicked,
                        onChangeLimitClicked = viewModel::onChangeLimitClicked
                    )
                }
            }
        }
    }
}

@Composable
fun BlockerScreen(
    uiState: BlockerUiState,
    onSnoozeClicked: () -> Unit,
    onImDoneClicked: () -> Unit,
    onChangeLimitClicked: () -> Unit,
) {
    BackHandler {
        onImDoneClicked()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black.copy(alpha = 0.3f) // Dimming effect
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.PaddingLarge), // Margin around the dialog
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 400.dp) // Constrain max width on large screens
                ) {
                    // Scrollable content area
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.PaddingExtraLarge),
                        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                    ) {
                        // Header
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                        ) {
                            DrawableImage(
                                drawable = uiState.appIcon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = uiState.title,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = uiState.description,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(Dimens.PaddingMedium))

                        // Informational sections
                        uiState.infoItems.forEach { item ->
                            PermissionInfoRow(
                                icon = item.icon,
                                title = item.title,
                                description = item.description
                            )
                        }
                    }

                    // Button bar at the bottom
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingSmall),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onChangeLimitClicked) {
                            Text("Change Limit")
                        }
                        Spacer(Modifier.width(Dimens.PaddingSmall))
                        TextButton(onClick = onImDoneClicked) {
                            Text("I'm Done")
                        }
                        Spacer(Modifier.width(Dimens.PaddingSmall))
                        Button(onClick = onSnoozeClicked) {
                            Text("Snooze 5 min")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionInfoRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(top = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockerScreenPreview() {
    HabitsTheme {
        BlockerScreen(
            uiState = BlockerUiState(
                appIcon = null,
                appName = "Sample App",
                title = "Time's up for Sample App",
                description = "You've reached your session limit of 30m.",
                infoItems = listOf(
                    InfoItem(
                        icon = Icons.Outlined.Timer,
                        title = "Session Limit: 30m",
                        description = "You set this goal to help manage your time on this app."
                    ),
                    InfoItem(
                        icon = Icons.Outlined.BarChart,
                        title = "Time Spent: 30m 5s",
                        description = "This screen is a reminder to take a break and stay mindful of your usage habits."
                    )
                )
            ),
            onSnoozeClicked = {},
            onImDoneClicked = {},
            onChangeLimitClicked = {}
        )
    }
}