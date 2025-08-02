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
import com.andrew264.habits.ui.usage.whitelist.assignColorForPackage
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
    val sessionLimitMinutes: Int?,
    // Screen Time
    val totalUsageMillis: Long,
    val usagePercentage: Float,
    val averageSessionMillis: Long,
    val screenTimeHistoricalData: List<BarChartEntry>,
    // Times Opened
    val timesOpened: Int,
    val timesOpenedHistoricalData: List<BarChartEntry>,
    // Common
    val peakUsageTimeLabel: String
)

data class UsageStatsUiState(
    val isLoading: Boolean = true,
    val isAppUsageTrackingEnabled: Boolean = true,
    val usageLimitNotificationsEnabled: Boolean = false,
    val isAppBlockingEnabled: Boolean = false,
    val sharedDailyUsageLimitMinutes: Int? = null,
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
                    usageLimitNotificationsEnabled = settings.usageLimitNotificationsEnabled,
                    isAppBlockingEnabled = settings.isAppBlockingEnabled,
                    sharedDailyUsageLimitMinutes = settings.sharedDailyUsageLimitMinutes
                )
            )
        } else {
            combine(
                getUsageStatisticsUseCase.execute(range),
                whitelistRepository.getWhitelistedApps(),
            ) { stats, whitelistedApps ->
                val allAppUsage = stats.totalUsagePerApp
                val allTimesOpened = stats.timesOpenedPerBin.flatMap { it.entries }
                    .groupBy({ it.key }, { it.value })
                    .mapValues { it.value.sum() }

                val appListSource = if (whitelistedApps.isEmpty()) {
                    (allAppUsage.keys + allTimesOpened.keys).distinct()
                } else {
                    whitelistedApps.map { it.packageName }
                }

                val appDetails = appListSource.mapNotNull { pkg ->
                    val whitelistedApp = whitelistedApps.find { it.packageName == pkg }
                    if (whitelistedApps.isNotEmpty() && whitelistedApp == null) return@mapNotNull null

                    val totalUsage = allAppUsage[pkg] ?: 0L
                    val timesOpened = allTimesOpened[pkg] ?: 0

                    val friendlyName = getAppName(pkg)
                    val usagePercentage = if (stats.totalScreenOnTime > 0) totalUsage.toFloat() / stats.totalScreenOnTime.toFloat() else 0f
                    val averageSessionMillis = if (timesOpened > 0) totalUsage / timesOpened else 0L

                    var peakUsage = 0L
                    var peakBin: UsageTimeBin? = null

                    val screenTimeHistoricalData = stats.timeBins.map { bin ->
                        val usageInBin = bin.appUsage[pkg] ?: 0L
                        if (usageInBin > peakUsage) {
                            peakUsage = usageInBin
                            peakBin = bin
                        }
                        val label = when (range) {
                            UsageTimeRange.DAY -> FormatUtils.formatChartHourLabel(bin.startTime)
                            UsageTimeRange.WEEK -> FormatUtils.formatChartDayLabel(bin.startTime)
                        }
                        BarChartEntry(value = usageInBin.toFloat(), label = label)
                    }

                    val timesOpenedHistoricalData = stats.timeBins.mapIndexed { index, bin ->
                        val opensInBin = stats.timesOpenedPerBin.getOrNull(index)?.get(pkg) ?: 0
                        val label = when (range) {
                            UsageTimeRange.DAY -> FormatUtils.formatChartHourLabel(bin.startTime)
                            UsageTimeRange.WEEK -> FormatUtils.formatChartDayLabel(bin.startTime)
                        }
                        BarChartEntry(value = opensInBin.toFloat(), label = label)
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
                        sessionLimitMinutes = whitelistedApp?.sessionLimitMinutes,
                        totalUsageMillis = totalUsage,
                        usagePercentage = usagePercentage,
                        averageSessionMillis = averageSessionMillis,
                        screenTimeHistoricalData = screenTimeHistoricalData,
                        timesOpened = timesOpened,
                        timesOpenedHistoricalData = timesOpenedHistoricalData,
                        peakUsageTimeLabel = peakUsageTimeLabel
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
                    isAppBlockingEnabled = settings.isAppBlockingEnabled,
                    sharedDailyUsageLimitMinutes = settings.sharedDailyUsageLimitMinutes,
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

    fun setAppBlockingEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAppBlockingEnabled(isEnabled)
        }
    }

    fun setSharedDailyLimit(minutes: Int?) {
        viewModelScope.launch {
            settingsRepository.updateSharedDailyUsageLimit(minutes)
        }
    }

    fun saveAppLimits(packageName: String, sessionLimit: Int?) {
        viewModelScope.launch {
            val existingApp = uiState.value.whitelistedApps.find { it.packageName == packageName }
            val updatedApp = existingApp?.copy(sessionLimitMinutes = sessionLimit)
                ?: WhitelistedApp(
                    packageName = packageName,
                    colorHex = assignColorForPackage(packageName),
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
            val existingApp = uiState.value.whitelistedApps.find { it.packageName == packageName }
            val updatedApp = existingApp?.copy(colorHex = colorHex)
                ?: WhitelistedApp(
                    packageName = packageName,
                    colorHex = colorHex,
                    sessionLimitMinutes = null
                )
            whitelistRepository.updateWhitelistedApp(updatedApp)
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