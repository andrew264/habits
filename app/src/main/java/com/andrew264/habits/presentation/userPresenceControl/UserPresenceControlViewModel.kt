package com.andrew264.habits.presentation.userPresenceControl

import androidx.lifecycle.ViewModel
import com.andrew264.habits.manager.UserPresenceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserPresenceControlViewModel @Inject constructor(
    private val userPresenceController: UserPresenceController
) : ViewModel() {

    fun onStartService() {
        userPresenceController.startService()
    }

    fun onStopService() {
        userPresenceController.stopService()
    }
}