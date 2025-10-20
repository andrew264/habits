package com.andrew264.habits.ui.schedule.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.andrew264.habits.R
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.schedule.SchedulesUiState
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SchedulesListPane(
    modifier: Modifier = Modifier,
    uiState: SchedulesUiState,
    onDelete: suspend (Schedule) -> Boolean,
    onEdit: (scheduleId: String) -> Unit,
    onNavigateUp: () -> Unit,
    onNewSchedule: () -> Unit,
    isDetailPaneVisible: Boolean,
    snackbarHostState: SnackbarHostState
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isDetailPaneVisible) {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.schedule_list_pane_schedules)) },
                    subtitle = { Text(stringResource(R.string.schedule_list_pane_manage_routines)) },
                    navigationIcon = {
                        val interactionSource = remember { MutableInteractionSource() }
                        HapticInteractionEffect(interactionSource)
                        IconButton(
                            onClick = onNavigateUp, interactionSource = interactionSource,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.top_app_bar_back_button_content_description))
                        }
                    },
                    actions = {
                        val interactionSource = remember { MutableInteractionSource() }
                        HapticInteractionEffect(interactionSource)
                        FilledTonalButton(
                            onClick = onNewSchedule,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.padding(end = Dimens.PaddingSmall),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.tertiary),
                            interactionSource = interactionSource,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.schedule_editor_new_schedule))
                            Spacer(Modifier.width(Dimens.PaddingSmall))
                            Text(stringResource(R.string.schedule_list_pane_create_schedule))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                ContainedLoadingIndicator()
            } else if (uiState.schedules.isEmpty() && uiState.schedulePendingDeletion == null) {
                EmptyState(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.schedule_list_pane_no_schedules_yet),
                    description = stringResource(R.string.schedule_list_pane_no_schedules_description)
                )
            } else {
                ScheduleList(
                    schedules = uiState.schedules,
                    listState = listState,
                    pendingDeletionId = uiState.schedulePendingDeletion?.id,
                    onDelete = onDelete,
                    onEdit = onEdit
                )
            }
        }
    }
}