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
                title = "Daily Goal Reached",
                description = "You've used your tracked apps for over $formattedLimit today.",
                infoItems = listOf(
                    InfoItem(
                        icon = Icons.Outlined.Timer,
                        title = "Daily Limit: $formattedLimit",
                        description = "This is the shared goal you set for all tracked apps."
                    ),
                    InfoItem(
                        icon = Icons.Outlined.BarChart,
                        title = "Total Time Spent: $formattedUsage",
                        description = "This screen is a reminder to take a break and stay mindful of your habits."
                    )
                )
            )

            else -> BlockerUiState( // "session"
                appIcon = appIcon,
                appName = appName,
                title = "Time for a Break",
                description = "You've used $appName for over $formattedLimit continuously.",
                infoItems = listOf(
                    InfoItem(
                        icon = Icons.Outlined.Timer,
                        title = "Session Limit: $formattedLimit",
                        description = "You set this goal to help manage your time in the app."
                    ),
                    InfoItem(
                        icon = Icons.Outlined.BarChart,
                        title = "Current Session: $formattedUsage",
                        description = "This screen is a reminder to take a break and use your time intentionally."
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