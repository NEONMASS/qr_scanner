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

// NEW: Expanded sorting options for the dropdown menu!
enum class SortOrder { RISK_HIGH_FIRST, RISK_LOW_FIRST, APP_NAME_AZ, PACKAGE_NAME }

class AuditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _installedApps = MutableStateFlow<List<AppPrivacyInfo>>(emptyList())
    val installedApps: StateFlow<List<AppPrivacyInfo>> = _installedApps.asStateFlow()

    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.RISK_HIGH_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

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

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applySorting()
    }

    private fun applySorting() {
        _installedApps.value = when (_sortOrder.value) {
            SortOrder.RISK_HIGH_FIRST -> rawAppList.sortedByDescending { it.riskLevel }
            SortOrder.RISK_LOW_FIRST -> rawAppList.sortedBy { it.riskLevel } // NEW: Low risk at the top
            SortOrder.APP_NAME_AZ -> rawAppList.sortedBy { it.appName.lowercase() }
            SortOrder.PACKAGE_NAME -> rawAppList.sortedBy { it.packageName }
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

            rawAppList = appList
            applySorting() 
            _isLoading.value = false 
        }
    }
}
