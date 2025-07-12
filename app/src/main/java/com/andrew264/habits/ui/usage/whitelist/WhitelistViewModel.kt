package com.andrew264.habits.ui.usage.whitelist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrew264.habits.domain.repository.WhitelistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class WhitelistUiState(
    val isLoading: Boolean = true,
    val searchText: String = "",
    val showSystemApps: Boolean = false,
    val apps: List<InstalledAppInfo> = emptyList(),
    val whitelistedPackageNames: Set<String> = emptySet(),
)

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val whitelistRepository: WhitelistRepository
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _searchText = MutableStateFlow("")
    private val _showSystemApps = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<WhitelistUiState> = combine(
        _searchText,
        _showSystemApps,
        _allApps,
        _isLoading,
    ) { searchText, showSystemApps, allApps, isLoading ->
        object {
            val searchText = searchText
            val showSystemApps = showSystemApps
            val allApps = allApps
            val isLoading = isLoading
        }
    }.combine(whitelistRepository.getWhitelistedAppsMap()) { fourFlowsResult, whitelistedMap ->
        val filteredApps = fourFlowsResult.allApps.filter { appInfo ->
            (fourFlowsResult.showSystemApps || !appInfo.isSystemApp) &&
                    (fourFlowsResult.searchText.isBlank() || appInfo.friendlyName.contains(fourFlowsResult.searchText, ignoreCase = true))
        }.sortedWith(
            compareBy<InstalledAppInfo> { !whitelistedMap.containsKey(it.packageName) }
                .thenBy { it.friendlyName.lowercase() }
        )

        WhitelistUiState(
            isLoading = fourFlowsResult.isLoading,
            searchText = fourFlowsResult.searchText,
            showSystemApps = fourFlowsResult.showSystemApps,
            apps = filteredApps,
            whitelistedPackageNames = whitelistedMap.keys
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WhitelistUiState()
    )


    init {
        loadApps(context)
    }

    private fun loadApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val pm = context.packageManager
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appInfos = allApps.mapNotNull { appInfo ->
                // Ignore our own app and common system packages
                if (appInfo.packageName == context.packageName || appInfo.packageName.startsWith("com.android.")) {
                    return@mapNotNull null
                }

                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    friendlyName = appInfo.loadLabel(pm).toString(),
                    icon = try {
                        appInfo.loadIcon(pm)
                    } catch (_: Exception) {
                        null
                    },
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            _allApps.value = appInfos
            _isLoading.value = false
        }
    }

    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }

    fun onToggleShowSystemApps(show: Boolean) {
        _showSystemApps.value = show
    }

    fun onToggleWhitelist(
        app: InstalledAppInfo,
        isCurrentlyWhitelisted: Boolean
    ) {
        viewModelScope.launch {
            if (isCurrentlyWhitelisted) {
                whitelistRepository.unWhitelistApp(app.packageName)
            } else {
                val assignedColor = assignColorForPackage(app.packageName)
                whitelistRepository.whitelistApp(app.packageName, assignedColor)
            }
        }
    }
}

private val appColorPalette = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
    "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
    "#8BC34A", "#FFC107", "#FF9800", "#FF5722", "#795548",
    "#607D8B"
)


fun assignColorForPackage(packageName: String): String {
    val index = abs(packageName.hashCode()) % appColorPalette.size
    return appColorPalette[index]
}