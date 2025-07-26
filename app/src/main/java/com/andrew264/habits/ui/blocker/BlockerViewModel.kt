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
        val (limitTitle, limitDescription) = when (limitType) {
            "session" -> "Session Limit: $formattedLimit" to "You set this goal to help manage your time in the app."
            "daily" -> "Daily Limit: $formattedLimit" to "You set this goal to help manage your time on this app."
            else -> "Usage Limit Reached" to "You've reached the usage limit you set."
        }
        val formattedUsage = FormatUtils.formatDuration(timeUsedMs)

        val infoItems = listOf(
            InfoItem(
                icon = Icons.Outlined.Timer,
                title = limitTitle,
                description = limitDescription
            ),
            InfoItem(
                icon = Icons.Outlined.BarChart,
                title = "Time Spent: $formattedUsage",
                description = "This screen is a reminder to take a break and stay mindful of your usage habits."
            )
        )

        _uiState.value = BlockerUiState(
            appIcon = appIcon,
            appName = appName,
            title = "Time's up for $appName",
            description = "You've reached your $limitType limit of $formattedLimit.",
            infoItems = infoItems
        )
    }

    fun onSnoozeClicked() {
        viewModelScope.launch {
            snoozeManager.snoozeApp(packageName, TimeUnit.MINUTES.toMillis(5))
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