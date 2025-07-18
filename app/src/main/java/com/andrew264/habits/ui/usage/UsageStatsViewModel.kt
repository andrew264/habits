package com.andrew264.habits.ui.usage

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.UsageStatistics
import com.andrew264.habits.domain.model.UsageTimeBin
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WhitelistRepository
import com.andrew264.habits.domain.usecase.GetUsageStatisticsUseCase
import com.andrew264.habits.ui.common.charts.BarChartEntry
import com.andrew264.habits.ui.common.utils.FormatUtils
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
    val color: String,
    val totalUsageMillis: Long,
    val usagePercentage: Float,
    val sessionCount: Int,
    val averageSessionMillis: Long,
    val peakUsageTimeLabel: String,
    val historicalData: List<BarChartEntry>
)

data class UsageStatsUiState(
    val isLoading: Boolean = true,
    val isAppUsageTrackingEnabled: Boolean = true,
    val isAccessibilityServiceEnabled: Boolean = false,
    val selectedRange: UsageTimeRange = UsageTimeRange.DAY,
    val stats: UsageStatistics? = null,
    val whitelistedAppColors: Map<String, String> = emptyMap(),
    val appDetails: List<AppDetails> = emptyList(),
    val averageSessionMillis: Long = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UsageStatsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getUsageStatisticsUseCase: GetUsageStatisticsUseCase,
    private val whitelistRepository: WhitelistRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val packageManager = context.packageManager
    private val appNameCache = mutableMapOf<String, String>()

    private val _selectedRange = MutableStateFlow(UsageTimeRange.DAY)
    private val refreshTrigger = MutableStateFlow(0)
    private val _isAccessibilityEnabled = MutableStateFlow(false)

    val uiState: StateFlow<UsageStatsUiState> = combine(
        settingsRepository.settingsFlow,
        _selectedRange,
        refreshTrigger,
        _isAccessibilityEnabled
    ) { settings, range, _, isAccessibilityEnabled ->
        Triple(settings, range, isAccessibilityEnabled)
    }.flatMapLatest { (settings, range, isAccessibilityEnabled) ->
        if (!settings.isAppUsageTrackingEnabled) {
            flowOf(
                UsageStatsUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = false,
                    isAccessibilityServiceEnabled = isAccessibilityEnabled
                )
            )
        } else {
            combine(
                getUsageStatisticsUseCase.execute(range),
                whitelistRepository.getWhitelistedAppsMap(),
            ) { stats, whitelistedApps ->
                val allAppUsage = stats.totalUsagePerApp
                val appListSource = if (whitelistedApps.isEmpty()) allAppUsage.keys else whitelistedApps.keys

                val appDetails = appListSource.map { pkg ->
                    val totalUsage = allAppUsage[pkg] ?: 0L
                    val friendlyName = getAppName(pkg)
                    val sessionCount = stats.timeBins.count { it.appUsage.containsKey(pkg) }
                    val usagePercentage = if (stats.totalScreenOnTime > 0) totalUsage.toFloat() / stats.totalScreenOnTime.toFloat() else 0f
                    val averageSessionMillis = if (sessionCount > 0) totalUsage / sessionCount else 0L

                    var peakUsage = 0L
                    var peakBin: UsageTimeBin? = null
                    val historicalData = stats.timeBins.mapNotNull { bin ->
                        val usageInBin = bin.appUsage[pkg] ?: 0L
                        if (usageInBin > peakUsage) {
                            peakUsage = usageInBin
                            peakBin = bin
                        }
                        if (usageInBin > 0) {
                            val label = when (range) {
                                UsageTimeRange.DAY -> FormatUtils.formatChartHourLabel(bin.startTime)
                                UsageTimeRange.WEEK -> FormatUtils.formatChartDayLabel(bin.startTime)
                            }
                            BarChartEntry(value = usageInBin.toFloat(), label = label)
                        } else {
                            null
                        }
                    }

                    val peakUsageTimeLabel = peakBin?.let {
                        when (range) {
                            UsageTimeRange.DAY -> "Most used around ${FormatUtils.formatTimestamp(it.startTime, "ha")}"
                            UsageTimeRange.WEEK -> "Most used on ${FormatUtils.formatDayFullName(it.startTime)}"
                        }
                    } ?: "Not used in this period"

                    AppDetails(
                        packageName = pkg,
                        friendlyName = friendlyName,
                        color = whitelistedApps[pkg] ?: "#808080",
                        totalUsageMillis = totalUsage,
                        usagePercentage = usagePercentage,
                        sessionCount = sessionCount,
                        averageSessionMillis = averageSessionMillis,
                        peakUsageTimeLabel = peakUsageTimeLabel,
                        historicalData = historicalData
                    )
                }.sortedByDescending { it.totalUsageMillis }


                val averageSessionMillis = if (stats.pickupCount > 0) {
                    stats.totalScreenOnTime / stats.pickupCount
                } else {
                    0L
                }

                UsageStatsUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = true,
                    isAccessibilityServiceEnabled = isAccessibilityEnabled,
                    selectedRange = range,
                    stats = stats,
                    whitelistedAppColors = whitelistedApps,
                    appDetails = appDetails,
                    averageSessionMillis = averageSessionMillis
                )
            }.onStart {
                emit(uiState.value.copy(isLoading = true))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UsageStatsUiState()
    )

    init {
        updateAccessibilityStatus()
    }

    fun setTimeRange(range: UsageTimeRange) {
        _selectedRange.value = range
    }

    fun refresh() {
        updateAccessibilityStatus()
        refreshTrigger.value++
    }

    fun setAppColor(
        packageName: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            whitelistRepository.whitelistApp(packageName, colorHex)
        }
    }

    private fun getAppName(packageName: String): String {
        return appNameCache.getOrPut(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                packageName
            }
        }
    }

    private fun updateAccessibilityStatus() {
        _isAccessibilityEnabled.value = AccessibilityUtils.isAccessibilityServiceEnabled(context)
    }
}