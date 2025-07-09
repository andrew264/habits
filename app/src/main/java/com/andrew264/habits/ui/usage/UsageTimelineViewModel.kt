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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val packageManager = context.packageManager
    private val appDetailsCache = mutableMapOf<String, Pair<String, Drawable?>>()

    private val _selectedRange = MutableStateFlow(BedtimeChartRange.DAY)
    private val _appForColorPicker = MutableStateFlow<AppDetails?>(null)

    val uiState: StateFlow<UsageTimelineUiState> = settingsRepository.settingsFlow
        .flatMapLatest { settings ->
            if (!settings.isAppUsageTrackingEnabled) {
                flowOf(UsageTimelineUiState(isLoading = false, isAppUsageTrackingEnabled = false))
            } else {
                combine(
                    _selectedRange.flatMapLatest { range ->
                        val endTime = System.currentTimeMillis()
                        val startTime = endTime - range.durationMillis
                        getUsageTimelineUseCase.execute(startTime, endTime)
                    },
                    _appForColorPicker,
                    whitelistRepository.getWhitelistedAppsMap()
                ) { timelineModel, appForPicker, whitelistedAppsMap ->
                    val whitelistedPackageNames = whitelistedAppsMap.keys
                    val shouldFilter = whitelistedPackageNames.isNotEmpty()

                    // Filter app segments if the whitelist is not empty
                    val filteredScreenOnPeriods = if (shouldFilter) {
                        timelineModel.screenOnPeriods.map { period ->
                            period.copy(appSegments = period.appSegments.filter { segment ->
                                whitelistedPackageNames.contains(segment.packageName)
                            })
                        }
                    } else {
                        timelineModel.screenOnPeriods
                    }
                    val filteredTimelineModel = timelineModel.copy(screenOnPeriods = filteredScreenOnPeriods)

                    val aggregatedUsage = aggregateAppUsage(timelineModel)
                    val totalScreenOnTime = timelineModel.totalScreenOnTime

                    val appDetails = aggregatedUsage.mapNotNull { (pkg, usageStats) ->
                        if (shouldFilter && !whitelistedPackageNames.contains(pkg)) {
                            null
                        } else {
                            val (totalUsage, sessionCount) = usageStats
                            val details = getAppDetails(pkg)
                            val usagePercentage = if (totalScreenOnTime > 0) {
                                totalUsage.toFloat() / totalScreenOnTime.toFloat()
                            } else {
                                0f
                            }

                            AppDetails(
                                packageName = pkg,
                                friendlyName = details.first,
                                icon = details.second,
                                color = whitelistedAppsMap[pkg] ?: "#808080", // Default grey for non-whitelisted
                                totalUsageMillis = totalUsage,
                                sessionCount = sessionCount,
                                usagePercentage = usagePercentage
                            )
                        }
                    }.sortedByDescending { it.totalUsageMillis }

                    val averageSessionMillis = if (timelineModel.pickupCount > 0) {
                        totalScreenOnTime / timelineModel.pickupCount
                    } else {
                        0L
                    }

                    UsageTimelineUiState(
                        isLoading = false,
                        isAppUsageTrackingEnabled = true,
                        selectedRange = _selectedRange.value,
                        timelineModel = filteredTimelineModel,
                        appDetails = appDetails,
                        appForColorPicker = appForPicker,
                        totalScreenOnTime = totalScreenOnTime,
                        pickupCount = timelineModel.pickupCount,
                        averageSessionMillis = averageSessionMillis
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UsageTimelineUiState()
        )


    fun setTimeRange(range: BedtimeChartRange) {
        _selectedRange.value = range
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
}