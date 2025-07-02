package com.andrew264.habits.ui.permissions

import androidx.lifecycle.ViewModel
import com.andrew264.habits.domain.controller.UserPresenceController
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.repository.UserPresenceHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class UserPresenceControlViewModel @Inject constructor(
    private val userPresenceController: UserPresenceController,
    userPresenceHistoryRepository: UserPresenceHistoryRepository
) : ViewModel() {

    val presenceState: StateFlow<UserPresenceState> = userPresenceHistoryRepository.userPresenceState
    val isServiceActive: StateFlow<Boolean> = userPresenceHistoryRepository.isServiceActive

    fun onStartService() {
        userPresenceController.startService()
    }

    fun onStopService() {
        userPresenceController.stopService()
    }
}