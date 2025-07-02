package com.andrew264.habits.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.domain.usecase.StartPresenceMonitoringUseCase
import com.andrew264.habits.domain.usecase.StopPresenceMonitoringUseCase
import com.andrew264.habits.model.UserPresenceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserPresenceControlUiState(
    val presenceState: UserPresenceState = UserPresenceState.UNKNOWN,
    val isServiceActive: Boolean = false
)

@HiltViewModel
class UserPresenceControlViewModel @Inject constructor(
    userPresenceHistoryRepository: UserPresenceHistoryRepository,
    private val startPresenceMonitoringUseCase: StartPresenceMonitoringUseCase,
    private val stopPresenceMonitoringUseCase: StopPresenceMonitoringUseCase
) : ViewModel() {

    val uiState: StateFlow<UserPresenceControlUiState> = combine(
        userPresenceHistoryRepository.userPresenceState,
        userPresenceHistoryRepository.isServiceActive
    ) { presenceState, isServiceActive ->
        UserPresenceControlUiState(
            presenceState = presenceState,
            isServiceActive = isServiceActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPresenceControlUiState()
    )

    fun onStartService() {
        viewModelScope.launch {
            startPresenceMonitoringUseCase.execute()
        }
    }

    fun onStopService() {
        viewModelScope.launch {
            stopPresenceMonitoringUseCase.execute()
        }
    }
}