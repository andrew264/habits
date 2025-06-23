package com.andrew264.habits.presentation.userPresenceControl

import androidx.lifecycle.ViewModel
import com.andrew264.habits.manager.UserPresenceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserPresenceControlViewModel @Inject constructor(
    private val userPresenceControl: UserPresenceController
) : ViewModel() {
    fun onStartWithSleepApi(): Boolean = userPresenceControl.startServiceWithSleepApi()
    fun onStartWithHeuristics() = userPresenceControl.startServiceWithHeuristics()
    fun onStopService() = userPresenceControl.stopService()
}
