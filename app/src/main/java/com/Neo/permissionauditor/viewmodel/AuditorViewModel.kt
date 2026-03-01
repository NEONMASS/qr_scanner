package com.Neo.permissionauditor.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.Neo.permissionauditor.model.AppPrivacyInfo
import com.Neo.permissionauditor.model.RiskLevel

// NEW: Define the sorting options
enum class SortOrder { RISK, PACKAGE_NAME, APP_NAME }

class AuditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _installedApps = MutableStateFlow<List<AppPrivacyInfo>>(emptyList())
    val installedApps: StateFlow<List<AppPrivacyInfo>> = _installedApps.asStateFlow()

    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // NEW: Track the current sorting method (defaults to Risk Level)
    private val _sortOrder = MutableStateFlow(SortOrder.RISK)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // NEW: Store the raw, unsorted list of apps in memory so we can re-sort instantly
    private var rawAppList = listOf<AppPrivacyInfo>()

    init {
        loadApps()
    }

    fun toggleSystemApps(show: Boolean) {
        _showSystemApps.value = show
        loadApps() 
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // NEW: Function to change the sort order and instantly apply it
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applySorting()
    }

    private fun applySorting() {
        _installedApps.value = when (_sortOrder.value) {
            // sortedByDescending puts HIGH risk at the very top
            SortOrder.RISK -> rawAppList.sortedByDescending { it.riskLevel }
            // Sort alphabetically by the underlying company code / package name (e.g. com.google...)
            SortOrder.PACKAGE_NAME -> rawAppList.sortedBy { it.packageName }
            // Sort alphabetically by the normal app name
            SortOrder.APP_NAME -> rawAppList.sortedBy { it.appName.lowercase() }
        }
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true 

            val packageManager = getApplication<Application>().packageManager
            val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            val appList = mutableListOf<AppPrivacyInfo>()

            for (pack in packages) {
                val appInfo = pack.applicationInfo ?: continue
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                if (isSystemApp && !_showSystemApps.value) continue

                val requestedPermissions = pack.requestedPermissions ?: emptyArray()
                val totalPerms = requestedPermissions.size

                val hasCamera = requestedPermissions.contains("android.permission.CAMERA")
                val isCameraGranted = hasCamera && packageManager.checkPermission("android.permission.CAMERA", pack.packageName) == PackageManager.PERMISSION_GRANTED

                val hasLocation = requestedPermissions.contains("android.permission.ACCESS_FINE_LOCATION") || requestedPermissions.contains("android.permission.ACCESS_COARSE_LOCATION")
                val isLocationGranted = hasLocation && (packageManager.checkPermission("android.permission.ACCESS_FINE_LOCATION", pack.packageName) == PackageManager.PERMISSION_GRANTED || packageManager.checkPermission("android.permission.ACCESS_COARSE_LOCATION", pack.packageName) == PackageManager.PERMISSION_GRANTED)

                val hasMic = requestedPermissions.contains("android.permission.RECORD_AUDIO")
                val isMicGranted = hasMic && packageManager.checkPermission("android.permission.RECORD_AUDIO", pack.packageName) == PackageManager.PERMISSION_GRANTED

                val sensitiveCount = listOf(hasCamera, hasLocation, hasMic).count { it }
                val riskLevel = when (sensitiveCount) {
                    3 -> RiskLevel.HIGH
                    1, 2 -> RiskLevel.MEDIUM
                    else -> RiskLevel.LOW
                }

                appList.add(
                    AppPrivacyInfo(
                        appName = appInfo.loadLabel(packageManager).toString(),
                        packageName = pack.packageName,
                        isSystemApp = isSystemApp,
                        hasCameraAccess = hasCamera,
                        isCameraGranted = isCameraGranted,
                        hasLocationAccess = hasLocation,
                        isLocationGranted = isLocationGranted,
                        hasMicrophoneAccess = hasMic,
                        isMicrophoneGranted = isMicGranted,
                        totalPermissionsRequested = totalPerms,
                        riskLevel = riskLevel
                    )
                )
            }

            // Save the raw list, sort it, and hide the loading spinner
            rawAppList = appList
            applySorting() 
            _isLoading.value = false 
        }
    }
}
