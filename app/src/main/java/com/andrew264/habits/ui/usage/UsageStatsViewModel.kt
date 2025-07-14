package com.andrew264.habits.ui.usage

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.model.UsageStatistics
import com.andrew264.habits.domain.repository.SettingsRepository
import com.andrew264.habits.domain.repository.WhitelistRepository
import com.andrew264.habits.domain.usecase.GetUsageStatisticsUseCase
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

data class UsageStatsUiState(
    val isLoading: Boolean = true,
    val isAppUsageTrackingEnabled: Boolean = true,
    val isAccessibilityServiceEnabled: Boolean = false,
    val selectedRange: UsageTimeRange = UsageTimeRange.DAY,
    val stats: UsageStatistics? = null,
    val whitelistedAppColors: Map<String, String> = emptyMap(),
    val appDetails: List<AppDetails> = emptyList(),
    val appForColorPicker: AppDetails? = null,
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
    private val appDetailsCache = mutableMapOf<String, Pair<String, Drawable?>>()

    private val _selectedRange = MutableStateFlow(UsageTimeRange.DAY)
    private val _appForColorPicker = MutableStateFlow<AppDetails?>(null)
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
                _appForColorPicker
            ) { stats, whitelistedApps, appForPicker ->
                val appDetails = if (whitelistedApps.isEmpty()) {
                    // If whitelist is empty, show all apps with usage
                    stats.totalUsagePerApp.map { (pkg, totalUsage) ->
                        val details = getAppDetails(pkg)
                        val sessionCount = stats.timeBins.count { it.appUsage.containsKey(pkg) }
                        val usagePercentage = if (stats.totalScreenOnTime > 0) totalUsage.toFloat() / stats.totalScreenOnTime.toFloat() else 0f
                        AppDetails(
                            packageName = pkg,
                            friendlyName = details.first,
                            icon = details.second,
                            color = "#808080", // Default color
                            totalUsageMillis = totalUsage,
                            usagePercentage = usagePercentage,
                            sessionCount = sessionCount
                        )
                    }.sortedByDescending { it.totalUsageMillis }
                } else {
                    // If whitelist has items, show only those apps (even if usage is zero)
                    whitelistedApps.map { (pkg, color) ->
                        val totalUsage = stats.totalUsagePerApp[pkg] ?: 0L
                        val details = getAppDetails(pkg)
                        val sessionCount = stats.timeBins.count { it.appUsage.containsKey(pkg) }
                        val usagePercentage = if (stats.totalScreenOnTime > 0) totalUsage.toFloat() / stats.totalScreenOnTime.toFloat() else 0f
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
                    appForColorPicker = appForPicker,
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

    private fun updateAccessibilityStatus() {
        _isAccessibilityEnabled.value = AccessibilityUtils.isAccessibilityServiceEnabled(context)
    }
}