package com.andrew264.habits.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.navigation.sharedAxisXEnter
import com.andrew264.habits.ui.navigation.sharedAxisXExit
import com.andrew264.habits.ui.schedule.components.SchedulesListPane
import com.andrew264.habits.ui.theme.Dimens
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun SchedulesListDetailScreen(
    onNavigateUp: () -> Unit,
    viewModel: SchedulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SchedulesListDetailScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onDeleteSchedule = viewModel::onDeleteSchedule
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SchedulesListDetailScreen(
    uiState: SchedulesUiState,
    onNavigateUp: () -> Unit,
    onDeleteSchedule: suspend (Schedule) -> Boolean
) {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<ScheduleSelection>(
        adaptStrategies =
            ListDetailPaneScaffoldDefaults.adaptStrategies(
                extraPaneAdaptStrategy =
                    AdaptStrategy.Reflow(reflowUnder = ListDetailPaneScaffoldRole.Detail)
            )
    )
    val scope = rememberCoroutineScope()
    val selection = scaffoldNavigator.currentDestination?.contentKey
    val density = LocalDensity.current
    val slideDistance = remember(density) {
        with(density) { 30.dp.toPx() }.roundToInt()
    }


    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        listPane = {
            AnimatedPane(
                enterTransition = sharedAxisXEnter(forward = false, slideDistance = slideDistance),
                exitTransition = sharedAxisXExit(forward = true, slideDistance = slideDistance)
            ) {
                SchedulesListPane(
                    uiState = uiState,
                    onDelete = onDeleteSchedule,
                    onEdit = { scheduleId ->
                        scope.launch {
                            scaffoldNavigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = ScheduleSelection(scheduleId)
                            )
                        }
                    },
                    onNavigateUp = onNavigateUp,
                    onNewSchedule = {
                        scope.launch {
                            scaffoldNavigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = ScheduleSelection(scheduleId = UUID.randomUUID().toString())
                            )
                        }
                    },
                    isDetailPaneVisible = selection != null
                )
            }
        },
        detailPane = {
            AnimatedPane(
                enterTransition = sharedAxisXEnter(forward = true, slideDistance = slideDistance),
                exitTransition = sharedAxisXExit(forward = false, slideDistance = slideDistance)
            ) {
                if (selection != null) {
                    ScheduleEditorScreen(
                        scheduleId = selection.scheduleId,
                        onNavigateUp = { scope.launch { scaffoldNavigator.navigateBack() } }
                    )
                } else {
                    DetailPanePlaceholder()
                }
            }
        }
    )
}

@Composable
private fun DetailPanePlaceholder() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(Dimens.PaddingLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.schedules_list_detail_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}