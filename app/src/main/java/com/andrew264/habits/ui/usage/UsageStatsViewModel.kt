package com.andrew264.habits.ui.usage

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.UsageStatistics
import com.andrew264.habits.domain.model.UsageTimeBin
import com.andrew264.habits.domain.model.WhitelistedApp
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
    val dailyLimitMinutes: Int?,
    val sessionLimitMinutes: Int?,
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
    val usageLimitNotificationsEnabled: Boolean = false,
    val isAccessibilityServiceEnabled: Boolean = false,
    val selectedRange: UsageTimeRange = UsageTimeRange.DAY,
    val stats: UsageStatistics? = null,
    val whitelistedApps: List<WhitelistedApp> = emptyList(),
    val appDetails: List<AppDetails> = emptyList(),
    val averageSessionMillis: Long = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UsageStatsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getUsageStatisticsUseCase: GetUsageStatisticsUseCase,
    private val whitelistRepository: WhitelistRepository,
    private val settingsRepository: SettingsRepository
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
        object {
            val settings = settings
            val range = range
            val isAccessibilityEnabled = isAccessibilityEnabled
        }
    }.flatMapLatest { flowParams ->
        val settings = flowParams.settings
        val range = flowParams.range
        val isAccessibilityEnabled = flowParams.isAccessibilityEnabled

        if (!settings.isAppUsageTrackingEnabled) {
            flowOf(
                UsageStatsUiState(
                    isLoading = false,
                    isAppUsageTrackingEnabled = false,
                    isAccessibilityServiceEnabled = isAccessibilityEnabled,
                    usageLimitNotificationsEnabled = settings.usageLimitNotificationsEnabled
                )
            )
        } else {
            combine(
                getUsageStatisticsUseCase.execute(range),
                whitelistRepository.getWhitelistedApps(),
            ) { stats, whitelistedApps ->
                val allAppUsage = stats.totalUsagePerApp
                val appListSource = if (whitelistedApps.isEmpty()) allAppUsage.keys else whitelistedApps.map { it.packageName }

                val appDetails = appListSource.mapNotNull { pkg ->
                    val whitelistedApp = whitelistedApps.find { it.packageName == pkg }
                    val totalUsage = allAppUsage[pkg] ?: 0L

                    // If no whitelist, show all apps. If whitelist, only show apps on it.
                    if (whitelistedApps.isNotEmpty() && whitelistedApp == null) return@mapNotNull null

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
                        color = whitelistedApp?.colorHex ?: "#808080",
                        dailyLimitMinutes = whitelistedApp?.dailyLimitMinutes,
                        sessionLimitMinutes = whitelistedApp?.sessionLimitMinutes,
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
                    usageLimitNotificationsEnabled = settings.usageLimitNotificationsEnabled,
                    selectedRange = range,
                    stats = stats,
                    whitelistedApps = whitelistedApps,
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

    fun setUsageLimitNotificationsEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUsageLimitNotificationsEnabled(isEnabled)
        }
    }

    fun saveAppLimits(packageName: String, dailyLimit: Int?, sessionLimit: Int?) {
        viewModelScope.launch {
            val app = uiState.value.whitelistedApps.find { it.packageName == packageName } ?: return@launch
            val updatedApp = app.copy(
                dailyLimitMinutes = dailyLimit,
                sessionLimitMinutes = sessionLimit
            )
            whitelistRepository.updateWhitelistedApp(updatedApp)
        }
    }

    fun setAppColor(
        packageName: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            val app = uiState.value.whitelistedApps.find { it.packageName == packageName } ?: return@launch
            whitelistRepository.updateWhitelistedApp(app.copy(colorHex = colorHex))
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