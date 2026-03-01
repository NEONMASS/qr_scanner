package com.Neo.permissionauditor.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Neo.permissionauditor.model.AppPrivacyInfo
import com.Neo.permissionauditor.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuditorViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Holds the UI loading state (true when scanning)
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 2. Holds the list of scanned apps
    private val _installedApps = MutableStateFlow<List<AppPrivacyInfo>>(emptyList())
    val installedApps: StateFlow<List<AppPrivacyInfo>> = _installedApps.asStateFlow()

    // 3. Holds the state of the System Apps toggle switch
    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    init {
        // Start scanning automatically when the app opens
        loadApps()
    }

    // Called when the user flips the switch in the UI
    fun toggleSystemApps(show: Boolean) {
        _showSystemApps.value = show
        loadApps() // Rescan the apps with the new filter
    }

    private fun loadApps() {
        _isLoading.value = true // Show the loading spinner

        // Launch on a background thread so the UI remains buttery smooth
        viewModelScope.launch(Dispatchers.IO) {
            val packageManager = getApplication<Application>().packageManager
            
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            val appList = mutableListOf<AppPrivacyInfo>()

            for (pack in packages) {
                val isSystemApp = (pack.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0
                
                // Skip system apps if the toggle is set to false
                if (isSystemApp && !_showSystemApps.value) continue

                val requestedPermissions = pack.requestedPermissions ?: emptyArray()
                
                val hasCamera = requestedPermissions.contains("android.permission.CAMERA")
                val hasLocation = requestedPermissions.contains("android.permission.ACCESS_FINE_LOCATION") || 
                                  requestedPermissions.contains("android.permission.ACCESS_COARSE_LOCATION")
                val hasMic = requestedPermissions.contains("android.permission.RECORD_AUDIO")

                // Simple risk logic: 3 sensitive permissions = HIGH, 1-2 = MEDIUM, 0 = LOW
                val sensitiveCount = listOf(hasCamera, hasLocation, hasMic).count { it }
                val riskLevel = when (sensitiveCount) {
                    3 -> RiskLevel.HIGH
                    1, 2 -> RiskLevel.MEDIUM
                    else -> RiskLevel.LOW
                }

                appList.add(
                    AppPrivacyInfo(
                        appName = pack.applicationInfo?.loadLabel(packageManager).toString(),
                        packageName = pack.packageName,
                        isSystemApp = isSystemApp,
                        hasCameraAccess = hasCamera,
                        hasLocationAccess = hasLocation,
                        hasMicrophoneAccess = hasMic,
                        riskLevel = riskLevel
                    )
                )
            }

            // Sort so HIGH risk apps are at the top, then sort alphabetically by name
            val sortedList = appList.sortedWith(
                compareBy<AppPrivacyInfo> { it.riskLevel }.thenBy { it.appName }
            )

            // Push the result back to the UI state and hide the loading spinner
            _installedApps.value = sortedList
            _isLoading.value = false
        }
    }
}
