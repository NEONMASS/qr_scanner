package com.yourname.permissionauditor.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.permissionauditor.model.AppPrivacyInfo
import com.yourname.permissionauditor.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuditorViewModel : ViewModel() {

    // Holds the UI state. true when scanning, false when done.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Holds the list of scanned apps.
    private val _appList = MutableStateFlow<List<AppPrivacyInfo>>(emptyList())
    val appList: StateFlow<List<AppPrivacyInfo>> = _appList.asStateFlow()

    fun scanDevicePermissions(context: Context, showSystemApps: Boolean = false) {
        _isLoading.value = true
        
        // Launch a background thread so the UI doesn't freeze
        viewModelScope.launch(Dispatchers.IO) {
            val packageManager = context.packageManager
            val scannedApps = mutableListOf<AppPrivacyInfo>()

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            } else {
                PackageManager.GET_PERMISSIONS.toLong()
            }

            val installedPackages = packageManager.getInstalledPackages(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                } else {
                    PackageManager.GET_PERMISSIONS
                }
            )

            for (packageInfo in installedPackages) {
                val isSystemApp = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                
                // Skip system apps if the toggle is off
                if (isSystemApp && !showSystemApps) continue

                val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

                val hasCamera = requestedPermissions.contains("android.permission.CAMERA")
                val hasLocation = requestedPermissions.contains("android.permission.ACCESS_FINE_LOCATION") || 
                                  requestedPermissions.contains("android.permission.ACCESS_COARSE_LOCATION")
                val hasMicrophone = requestedPermissions.contains("android.permission.RECORD_AUDIO")

                // Calculate Risk Level
                val permissionCount = listOf(hasCamera, hasLocation, hasMicrophone).count { it }
                val risk = when (permissionCount) {
                    3 -> RiskLevel.HIGH
                    1, 2 -> RiskLevel.MEDIUM
                    else -> RiskLevel.LOW
                }

                // Only add apps that have at least one of the permissions we care about
                if (risk != RiskLevel.LOW) {
                    scannedApps.add(
                        AppPrivacyInfo(
                            appName = appName,
                            packageName = packageInfo.packageName,
                            isSystemApp = isSystemApp,
                            hasCameraAccess = hasCamera,
                            hasLocationAccess = hasLocation,
                            hasMicrophoneAccess = hasMicrophone,
                            riskLevel = risk
                        )
                    )
                }
            }

            // Sort by High Risk first, then alphabetically
            val sortedList = scannedApps.sortedWith(
                compareBy<AppPrivacyInfo> { it.riskLevel }.thenBy { it.appName }
            )

            // Push the result to the UI and hide the loading spinner
            _appList.value = sortedList
            _isLoading.value = false
        }
    }
}