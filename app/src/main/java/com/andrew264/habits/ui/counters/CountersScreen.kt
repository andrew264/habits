package com.andrew264.habits.ui.counters

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.components.ContainedLoadingIndicator
import com.andrew264.habits.ui.common.components.EmptyState
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.common.list_items.ContainedLazyColumn
import com.andrew264.habits.ui.counters.components.CounterListItem
import com.andrew264.habits.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CountersScreen(
    viewModel: CountersViewModel = hiltViewModel(),
    onNavigateToCreateCounter: () -> Unit,
    onNavigateToCounterDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.counters_screen_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            HapticInteractionEffect(interactionSource)
            FloatingActionButton(
                onClick = onNavigateToCreateCounter,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                interactionSource = interactionSource
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Counter")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                ContainedLoadingIndicator()
            } else if (uiState.counters.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Numbers,
                    title = stringResource(R.string.counters_empty_state_title),
                    description = stringResource(R.string.counters_empty_state_description),
                )
            } else {
                ContainedLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.PaddingLarge),
                    items = uiState.counters,
                    key = { it.counter.id },
                    onItemClick = { item -> onNavigateToCounterDetail(item.counter.id) }
                ) { item -> CounterListItem(item = item) }
            }
        }
    }
}