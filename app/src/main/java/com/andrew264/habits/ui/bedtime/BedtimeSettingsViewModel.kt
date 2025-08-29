package com.andrew264.habits.ui.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.PersistentSettings
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.usecase.SetSleepScheduleUseCase
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.andrew264.habits.model.schedule.Schedule
import com.andrew264.habits.ui.theme.createPreviewPersistentSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BedtimeSettingsUiState(
    val settings: PersistentSettings = createPreviewPersistentSettings(),
    val allSchedules: List<Schedule> = emptyList(),
)

sealed interface BedtimeSettingsEvent {
    object RequestActivityPermission : BedtimeSettingsEvent
}

@HiltViewModel
class BedtimeSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val setSleepScheduleUseCase: SetSleepScheduleUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<BedtimeSettingsEvent>()
    val events = _events.asSharedFlow()

    private val allSchedulesFlow = scheduleRepository.getAllSchedules()
        .map { dbSchedules ->
            listOf(DefaultSchedules.defaultSleepSchedule) + dbSchedules
        }

    val uiState: StateFlow<BedtimeSettingsUiState> = combine(
        settingsRepository.settingsFlow,
        allSchedulesFlow
    ) { settings, schedules ->
        BedtimeSettingsUiState(
            settings = settings,
            allSchedules = schedules,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BedtimeSettingsUiState()
    )

    fun onBedtimeTrackingToggled(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                _events.emit(BedtimeSettingsEvent.RequestActivityPermission)
            } else {
                settingsRepository.updateBedtimeTrackingEnabled(false)
            }
        }
    }

    fun onScheduleSelected(schedule: Schedule) {
        viewModelScope.launch {
            setSleepScheduleUseCase.execute(schedule.id)
        }
    }
}