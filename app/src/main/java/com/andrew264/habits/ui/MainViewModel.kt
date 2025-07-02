package com.andrew264.habits.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.usecase.HandlePermissionResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val destinationRoute: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val handlePermissionResultUseCase: HandlePermissionResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var initialPermissionCheckDone = false

    fun handleIntent(intent: Intent?) {
        if (intent == null || (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER))) {
            return
        }
        val destination = intent.getStringExtra("destination_route")
        if (destination != null) {
            _uiState.update { it.copy(destinationRoute = destination) }
        }
    }

    fun onRouteConsumed() {
        _uiState.update { it.copy(destinationRoute = null) }
    }

    fun handlePermissionResults(
        activityRecognitionGranted: Boolean,
        notificationsGranted: Boolean
    ) {
        viewModelScope.launch {
            handlePermissionResultUseCase.execute(activityRecognitionGranted, notificationsGranted)
        }
        initialPermissionCheckDone = true
    }

    /**
     * Checks if the initial permission request is needed.
     * This is designed to be called once from a LaunchedEffect.
     */
    fun needsInitialPermissionCheck(): Boolean {
        return !initialPermissionCheckDone
    }
}