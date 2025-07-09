package com.andrew264.habits.domain.usecase

import android.util.Log
import com.andrew264.habits.domain.analyzer.ScheduleAnalyzer
import com.andrew264.habits.domain.repository.ScheduleRepository
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.UserPresenceHistoryRepository
import com.andrew264.habits.model.UserPresenceState
import com.andrew264.habits.model.schedule.DefaultSchedules
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * A sealed class representing the different triggers for a presence state evaluation.
 */
sealed class PresenceEvaluationInput {
    object ScreenOn : PresenceEvaluationInput()
    object ScreenOff : PresenceEvaluationInput()
    object UserPresent : PresenceEvaluationInput()
    object InitialEvaluation : PresenceEvaluationInput()
    data class SleepApiSegment(val event: SleepSegmentEvent) : PresenceEvaluationInput()
    data class SleepApiClassify(val event: SleepClassifyEvent) : PresenceEvaluationInput()
}

/**
 * Encapsulates all business logic for determining the user's presence state (AWAKE, SLEEPING, UNKNOWN).
 * This use case is the single source of truth for state evaluation decisions.
 */
class EvaluateUserPresenceUseCase @Inject constructor(
    private val userPresenceHistoryRepository: UserPresenceHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository
) {

    suspend fun execute(input: PresenceEvaluationInput) {
        val oldState = userPresenceHistoryRepository.userPresenceState.value
        var newState = oldState
        var reason = "No change"

        // Fetch the current schedule for analysis
        val settings = settingsRepository.settingsFlow.first()
        val scheduleId = settings.selectedScheduleId ?: DefaultSchedules.DEFAULT_SLEEP_SCHEDULE_ID
        val schedule = scheduleRepository.getSchedule(scheduleId).first() ?: DefaultSchedules.defaultSleepSchedule
        val scheduleAnalyzer = ScheduleAnalyzer(schedule.groups)
        val isScheduledTime = scheduleAnalyzer.isCurrentTimeInSchedule()
        val isBedtimeTrackingEnabled = settings.isBedtimeTrackingEnabled

        Log.d("EvaluateUserPresenceUseCase", "Evaluating state. Input: ${input::class.simpleName}, Old State: $oldState, Is Scheduled Sleep: $isScheduledTime")

        when (input) {
            is PresenceEvaluationInput.SleepApiSegment -> {
                if (input.event.status == SleepSegmentEvent.STATUS_SUCCESSFUL) {
                    val now = System.currentTimeMillis()
                    if (now in input.event.startTimeMillis..input.event.endTimeMillis) {
                        newState = UserPresenceState.SLEEPING
                        reason = "Sleep API: In sleep segment"
                    } else if (now > input.event.endTimeMillis && now < input.event.endTimeMillis + TimeUnit.MINUTES.toMillis(15)) {
                        newState = UserPresenceState.AWAKE
                        reason = "Sleep API: Just woke up from sleep segment"
                    }
                }
            }

            is PresenceEvaluationInput.SleepApiClassify -> {
                if (input.event.confidence >= 75 && (input.event.light <= 1 || input.event.motion <= 1)) {
                    newState = UserPresenceState.SLEEPING
                    reason = "Sleep API: High confidence sleep classification"
                }
            }

            is PresenceEvaluationInput.ScreenOn, is PresenceEvaluationInput.UserPresent -> {
                if (!isBedtimeTrackingEnabled || !isScheduledTime) {
                    newState = UserPresenceState.AWAKE
                    reason = "Device interaction outside of scheduled sleep time"
                }
            }

            is PresenceEvaluationInput.ScreenOff, is PresenceEvaluationInput.InitialEvaluation -> {
                if (!isBedtimeTrackingEnabled || input is PresenceEvaluationInput.InitialEvaluation) {
                    if (isScheduledTime) {
                        newState = UserPresenceState.SLEEPING
                        reason = "Heuristic: Scheduled time and screen off/initial evaluation"
                    } else {
                        newState = UserPresenceState.AWAKE
                        reason = "Heuristic: Outside scheduled time"
                    }
                }
            }
        }

        if (newState != oldState) {
            updateUserPresenceStateInternal(newState, reason)
        } else {
            Log.d("EvaluateUserPresenceUseCase", "State unchanged: $oldState ($reason)")
        }
    }

    private fun updateUserPresenceStateInternal(
        newState: UserPresenceState,
        reason: String
    ) {
        userPresenceHistoryRepository.updateUserPresenceState(newState)
        Log.i("EvaluateUserPresenceUseCase", "STATE CHANGE: User presence -> $newState (Reason: $reason)")
    }
}