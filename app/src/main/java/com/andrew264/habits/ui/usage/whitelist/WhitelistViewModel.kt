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

data class WhitelistUiState(
    val isLoading: Boolean = true,
    val searchText: String = "",
    val showSystemApps: Boolean = false,
    val apps: List<InstalledAppInfo> = emptyList(),
    val whitelistedPackageNames: Set<String> = emptySet(),
    val appPendingColorSelection: InstalledAppInfo? = null
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
    private val _appPendingColorSelection = MutableStateFlow<InstalledAppInfo?>(null)

    val uiState: StateFlow<WhitelistUiState> = combine(
        _searchText,
        _showSystemApps,
        _allApps,
        _isLoading,
        _appPendingColorSelection
    ) { searchText, showSystemApps, allApps, isLoading, pendingApp ->
        // Create an intermediate anonymous object to pass the results to the next combine
        object {
            val searchText = searchText
            val showSystemApps = showSystemApps
            val allApps = allApps
            val isLoading = isLoading
            val pendingApp = pendingApp
        }
    }.combine(whitelistRepository.getWhitelistedAppsMap()) { fiveFlowsResult, whitelistedMap ->
        val filteredApps = fiveFlowsResult.allApps.filter { appInfo ->
            (fiveFlowsResult.showSystemApps || !appInfo.isSystemApp) &&
                    (fiveFlowsResult.searchText.isBlank() || appInfo.friendlyName.contains(fiveFlowsResult.searchText, ignoreCase = true))
        }.sortedBy { it.friendlyName.lowercase() }

        WhitelistUiState(
            isLoading = fiveFlowsResult.isLoading,
            searchText = fiveFlowsResult.searchText,
            showSystemApps = fiveFlowsResult.showSystemApps,
            apps = filteredApps,
            whitelistedPackageNames = whitelistedMap.keys,
            appPendingColorSelection = fiveFlowsResult.pendingApp
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
                // Ignore our own app
                if (appInfo.packageName == context.packageName) return@mapNotNull null

                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    friendlyName = appInfo.loadLabel(pm).toString(),
                    icon = try { appInfo.loadIcon(pm) } catch (_: Exception) { null },
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

    fun onToggleWhitelist(app: InstalledAppInfo, isCurrentlyWhitelisted: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyWhitelisted) {
                whitelistRepository.unWhitelistApp(app.packageName)
            } else {
                _appPendingColorSelection.value = app
            }
        }
    }

    fun onColorSelected(packageName: String, color: String) {
        viewModelScope.launch {
            // Default color is white, which is a bit hard to see. Let's pick a default.
            val finalColor = if (color == "#FFFFFF") "#9E9E9E" else color
            whitelistRepository.whitelistApp(packageName, finalColor)
            _appPendingColorSelection.value = null
        }
    }

    fun onDismissColorPicker() {
        _appPendingColorSelection.value = null
    }
}