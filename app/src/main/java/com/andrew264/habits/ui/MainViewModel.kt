package com.andrew264.habits.ui

import android.Manifest
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.usecase.HandlePermissionResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val destinationRoute: String? = null,
    val settings: PersistentSettings? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val handlePermissionResultUseCase: HandlePermissionResultUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var initialPermissionCheckDone = false

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

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

    fun handlePermissionResults(permissions: Map<String, Boolean>) {
        viewModelScope.launch {
            val notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            val activityRecognitionGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false

            handlePermissionResultUseCase.execute(notificationsGranted)

            if (activityRecognitionGranted) {
                settingsRepository.updateBedtimeTrackingEnabled(true)
            }
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