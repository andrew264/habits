package com.andrew264.habits.ui.blocker

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.R
import com.andrew264.habits.domain.manager.SnoozeManager
import com.andrew264.habits.domain.usecase.CheckUsageLimitsUseCase
import com.andrew264.habits.ui.common.utils.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class InfoItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

data class BlockerUiState(
    val appIcon: android.graphics.drawable.Drawable?,
    val appName: String,
    val title: String,
    val description: String,
    val infoItems: List<InfoItem>
)

sealed interface BlockerEvent {
    data class NavigateToAppDetails(val packageName: String) : BlockerEvent
    object NavigateToHome : BlockerEvent
    object Finish : BlockerEvent
}

@HiltViewModel
class BlockerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val snoozeManager: SnoozeManager
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>(CheckUsageLimitsUseCase.EXTRA_PACKAGE_NAME)!!
    private val limitType: String = savedStateHandle.get<String>(CheckUsageLimitsUseCase.EXTRA_LIMIT_TYPE)!!
    private val timeUsedMs: Long = savedStateHandle.get<Long>(CheckUsageLimitsUseCase.EXTRA_TIME_USED_MS)!!
    private val limitMinutes: Int = savedStateHandle.get<Int>(CheckUsageLimitsUseCase.EXTRA_LIMIT_MINUTES)!!

    private val _uiState = MutableStateFlow<BlockerUiState?>(null)
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BlockerEvent>()
    val events = _events.asSharedFlow()

    init {
        val pm = context.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
        val appIcon = try {
            pm.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        val formattedLimit = FormatUtils.formatDuration(TimeUnit.MINUTES.toMillis(limitMinutes.toLong()))
        val formattedUsage = FormatUtils.formatDuration(timeUsedMs)

        _uiState.value = when (limitType) {
            "daily_shared" -> BlockerUiState(
                appIcon = appIcon,
                appName = appName,
                title = context.getString(R.string.blocker_daily_goal_reached_title),
                description = context.getString(R.string.blocker_daily_goal_reached_description, formattedLimit),
                infoItems = listOf(
                    InfoItem(
                        icon = Icons.Outlined.Timer,
                        title = context.getString(R.string.blocker_daily_limit_title, formattedLimit),
                        description = context.getString(R.string.blocker_daily_limit_description)
                    ),
                    InfoItem(
                        icon = Icons.Outlined.BarChart,
                        title = context.getString(R.string.blocker_total_time_spent_title, formattedUsage),
                        description = context.getString(R.string.blocker_mindful_usage_reminder)
                    )
                )
            )

            else -> BlockerUiState( // "session"
                appIcon = appIcon,
                appName = appName,
                title = context.getString(R.string.blocker_session_limit_title),
                description = context.getString(R.string.blocker_session_limit_description, appName, formattedLimit),
                infoItems = listOf(
                    InfoItem(
                        icon = Icons.Outlined.Timer,
                        title = context.getString(R.string.blocker_session_limit_details_title, formattedLimit),
                        description = context.getString(R.string.blocker_session_limit_details_description)
                    ),
                    InfoItem(
                        icon = Icons.Outlined.BarChart,
                        title = context.getString(R.string.blocker_current_session_title, formattedUsage),
                        description = context.getString(R.string.blocker_intentional_usage_reminder)
                    )
                )
            )
        }
    }

    fun onSnoozeClicked() {
        viewModelScope.launch {
            if (limitType == "daily_shared") {
                snoozeManager.snoozeDailyLimit(TimeUnit.MINUTES.toMillis(5))
            } else {
                snoozeManager.snoozeApp(packageName, TimeUnit.MINUTES.toMillis(5))
            }
            _events.emit(BlockerEvent.Finish)
        }
    }

    fun onImDoneClicked() {
        viewModelScope.launch {
            _events.emit(BlockerEvent.NavigateToHome)
        }
    }

    fun onChangeLimitClicked() {
        viewModelScope.launch {
            _events.emit(BlockerEvent.NavigateToAppDetails(packageName))
        }
    }
}