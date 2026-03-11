package com.andrew264.habits.ui.counters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.usecase.counter.CounterWithProgress
import com.andrew264.habits.domain.usecase.counter.GetCountersWithTodayProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CountersUiState(
    val isLoading: Boolean = true,
    val counters: List<CounterWithProgress> = emptyList()
)

@HiltViewModel
class CountersViewModel @Inject constructor(
    getCountersWithTodayProgressUseCase: GetCountersWithTodayProgressUseCase
) : ViewModel() {

    val uiState: StateFlow<CountersUiState> = getCountersWithTodayProgressUseCase.execute()
        .map { counters ->
            CountersUiState(
                isLoading = false,
                counters = counters
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CountersUiState(isLoading = true)
        )
}