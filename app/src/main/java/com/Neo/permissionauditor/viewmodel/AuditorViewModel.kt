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

class AuditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _installedApps = MutableStateFlow<List<AppPrivacyInfo>>(emptyList())
    val installedApps: StateFlow<List<AppPrivacyInfo>> = _installedApps.asStateFlow()

    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    // NEW: State to hold the current search text
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadApps()
    }

    fun toggleSystemApps(show: Boolean) {
        _showSystemApps.value = show
        loadApps() 
    }

    // NEW: Function to update the search text as you type
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
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

            _installedApps.value = appList.sortedBy { it.riskLevel }
            _isLoading.value = false 
        }
    }
}
