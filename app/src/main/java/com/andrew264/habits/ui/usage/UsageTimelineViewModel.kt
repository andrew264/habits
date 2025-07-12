package com.andrew264.habits.ui.usage

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.UsageTimelineModel
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WhitelistRepository
import com.andrew264.habits.domain.usecase.GetUsageTimelineUseCase
import com.andrew264.habits.ui.bedtime.BedtimeChartRange
import com.andrew264.habits.util.AccessibilityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDetails(
    val packageName: String,
    val friendlyName: String,
    val icon: Drawable?,
    val color: String,
    val totalUsageMillis: Long,
    val usagePercentage: Float,
    val sessionCount: Int,
)

data class UsageTimelineUiState(
    val isLoading: Boolean = true,
    val isAppUsageTrackingEnabled: Boolean = true,
    val isAccessibilityServiceEnabled: Boolean = false,
    val selectedRange: BedtimeChartRange = BedtimeChartRange.DAY,
    val timelineModel: UsageTimelineModel? = null,
    val appDetails: List<AppDetails> = emptyList(),
    val appForColorPicker: AppDetails? = null,
    val totalScreenOnTime: Long = 0,
    val pickupCount: Int = 0,
    val averageSessionMillis: Long = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UsageTimelineViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getUsageTimelineUseCase: GetUsageTimelineUseCase,
    private val whitelistRepository: WhitelistRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val packageManager = context.packageManager
    private val appDetailsCache = mutableMapOf<String, Pair<String, Drawable?>>()

    private val _selectedRange = MutableStateFlow(BedtimeChartRange.DAY)
    private val _appForColorPicker = MutableStateFlow<AppDetails?>(null)
    private val refreshTrigger = MutableStateFlow(0)
    private val _isAccessibilityEnabled = MutableStateFlow(false)

    val uiState: StateFlow<UsageTimelineUiState> = combine(
        settingsRepository.settingsFlow,
        _selectedRange,
        refreshTrigger,
        _isAccessibilityEnabled
    ) { settings, range, _, isAccessibilityEnabled ->
        object {
            val settings = settings
            val range = range
            val isAccessibilityEnabled = isAccessibilityEnabled
            val now = System.currentTimeMillis()
        }
    }.flatMapLatest { captured ->
        if (!captured.settings.isAppUsageTrackingEnabled) {
            flowOf(
                UsageTimelineUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = false,
                    isAccessibilityServiceEnabled = captured.isAccessibilityEnabled
                )
            )
        } else {
            val startTime = captured.now - captured.range.durationMillis
            combine(
                getUsageTimelineUseCase.execute(startTime, captured.now),
                _appForColorPicker,
                whitelistRepository.getWhitelistedAppsMap()
            ) { timelineModel, appForPicker, whitelistedAppsMap ->
                val totalScreenOnTime = timelineModel.totalScreenOnTime
                val aggregatedUsage = aggregateAppUsage(timelineModel)

                val appDetails = if (whitelistedAppsMap.isEmpty()) {
                    // If whitelist is empty, show all apps with usage
                    aggregatedUsage.map { (pkg, usageStats) ->
                        val (totalUsage, sessionCount) = usageStats
                        val details = getAppDetails(pkg)
                        val usagePercentage = if (totalScreenOnTime > 0) totalUsage.toFloat() / totalScreenOnTime.toFloat() else 0f
                        AppDetails(
                            packageName = pkg,
                            friendlyName = details.first,
                            icon = details.second,
                            color = "#808080", // Default color for non-whitelisted apps
                            totalUsageMillis = totalUsage,
                            usagePercentage = usagePercentage,
                            sessionCount = sessionCount
                        )
                    }.sortedByDescending { it.totalUsageMillis }
                } else {
                    // If whitelist exists, show all whitelisted apps
                    whitelistedAppsMap.map { (pkg, color) ->
                        val usageStats = aggregatedUsage[pkg] ?: Pair(0L, 0)
                        val (totalUsage, sessionCount) = usageStats
                        val details = getAppDetails(pkg)
                        val usagePercentage = if (totalScreenOnTime > 0) totalUsage.toFloat() / totalScreenOnTime.toFloat() else 0f
                        AppDetails(
                            packageName = pkg,
                            friendlyName = details.first,
                            icon = details.second,
                            color = color,
                            totalUsageMillis = totalUsage,
                            usagePercentage = usagePercentage,
                            sessionCount = sessionCount
                        )
                    }.sortedByDescending { it.totalUsageMillis }
                }

                val averageSessionMillis = if (timelineModel.pickupCount > 0) {
                    totalScreenOnTime / timelineModel.pickupCount
                } else {
                    0L
                }

                UsageTimelineUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = true,
                    isAccessibilityServiceEnabled = captured.isAccessibilityEnabled,
                    selectedRange = captured.range,
                    timelineModel = timelineModel,
                    appDetails = appDetails,
                    appForColorPicker = appForPicker,
                    totalScreenOnTime = totalScreenOnTime,
                    pickupCount = timelineModel.pickupCount,
                    averageSessionMillis = averageSessionMillis
                )
            }.onStart {
                emit(uiState.value.copy(isLoading = true))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UsageTimelineUiState()
    )

    init {
        updateAccessibilityStatus()
    }

    fun setTimeRange(range: BedtimeChartRange) {
        _selectedRange.value = range
    }

    fun refresh() {
        updateAccessibilityStatus()
        refreshTrigger.value++
    }

    fun showColorPickerForApp(app: AppDetails) {
        _appForColorPicker.value = app
    }

    fun dismissColorPicker() {
        _appForColorPicker.value = null
    }

    fun setAppColor(
        packageName: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            whitelistRepository.whitelistApp(packageName, colorHex)
        }
    }

    private fun getAppDetails(packageName: String): Pair<String, Drawable?> {
        return appDetailsCache.getOrPut(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val friendlyName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(packageName)
                Pair(friendlyName, icon)
            } catch (_: PackageManager.NameNotFoundException) {
                Pair(packageName, null)
            }
        }
    }

    private fun aggregateAppUsage(model: UsageTimelineModel): Map<String, Pair<Long, Int>> {
        return model.screenOnPeriods
            .flatMap { it.appSegments }
            .groupBy { it.packageName }
            .mapValues { (_, segments) ->
                val totalUsage = segments.sumOf { it.endTimestamp - it.startTimestamp }
                val sessionCount = segments.size
                Pair(totalUsage, sessionCount)
            }
    }

    private fun updateAccessibilityStatus() {
        _isAccessibilityEnabled.value = AccessibilityUtils.isAccessibilityServiceEnabled(context)
    }
}