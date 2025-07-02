package com.andrew264.habits.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.domain.usecase.StartPresenceMonitoringUseCase
import com.andrew264.habits.domain.usecase.StopPresenceMonitoringUseCase
import com.andrew264.habits.model.UserPresenceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPresenceControlViewModel @Inject constructor(
    userPresenceHistoryRepository: UserPresenceHistoryRepository,
    private val startPresenceMonitoringUseCase: StartPresenceMonitoringUseCase,
    private val stopPresenceMonitoringUseCase: StopPresenceMonitoringUseCase
) : ViewModel() {

    val presenceState: StateFlow<UserPresenceState> = userPresenceHistoryRepository.userPresenceState
    val isServiceActive: StateFlow<Boolean> = userPresenceHistoryRepository.isServiceActive

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