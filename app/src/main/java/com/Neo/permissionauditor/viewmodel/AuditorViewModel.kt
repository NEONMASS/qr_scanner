package com.Neo.permissionauditor.viewmodel // <-- Fixed lowercase 'p'

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

    // Explicit loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadApps()
    }

    fun toggleSystemApps(show: Boolean) {
        _showSystemApps.value = show
        loadApps() // Trigger a fresh scan when toggled
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true // Turn on the loading spinner

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

                val hasCamera = requestedPermissions.contains("android.permission.CAMERA")
                val hasLocation = requestedPermissions.contains("android.permission.ACCESS_FINE_LOCATION") || 
                                  requestedPermissions.contains("android.permission.ACCESS_COARSE_LOCATION")
                val hasMic = requestedPermissions.contains("android.permission.RECORD_AUDIO")

                val sensitiveCount = listOf(hasCamera, hasLocation, hasMic).count { it }
                val riskLevel = when (sensitiveCount) {
                    3 -> RiskLevel.HIGH
                    1, 2 -> RiskLevel.MEDIUM
                    else -> RiskLevel.LOW
                }

                val appName = appInfo.loadLabel(packageManager).toString()

                appList.add(
                    AppPrivacyInfo(
                        appName = appName,
                        packageName = pack.packageName,
                        isSystemApp = isSystemApp,
                        hasCameraAccess = hasCamera,
                        hasLocationAccess = hasLocation,
                        hasMicrophoneAccess = hasMic,
                        riskLevel = riskLevel
                    )
                )
            }

            _installedApps.value = appList.sortedBy { it.riskLevel }
            _isLoading.value = false // Turn off the loading spinner
        }
    }
}
