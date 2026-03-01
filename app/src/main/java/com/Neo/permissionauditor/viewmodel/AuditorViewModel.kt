package com.Neo.permissionauditor.viewmodel

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
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

enum class SortOrder { RISK_HIGH_FIRST, RISK_LOW_FIRST, APP_NAME_AZ, PACKAGE_NAME, USAGE_MOST_USED }

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

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private var rawAppList = listOf<AppPrivacyInfo>()

    init {
        checkPermissionAndLoadApps()
    }

    fun toggleSystemApps(show: Boolean) {
        _showSystemApps.value = show
        checkPermissionAndLoadApps() 
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applySorting()
    }

    fun checkPermissionAndLoadApps() {
        val appOps = getApplication<Application>().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getApplication<Application>().packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getApplication<Application>().packageName)
        }
        
        _hasUsagePermission.value = (mode == AppOpsManager.MODE_ALLOWED)
        loadApps()
    }

    private fun applySorting() {
        _installedApps.value = when (_sortOrder.value) {
            SortOrder.RISK_HIGH_FIRST -> rawAppList.sortedByDescending { it.riskLevel }
            SortOrder.RISK_LOW_FIRST -> rawAppList.sortedBy { it.riskLevel }
            SortOrder.APP_NAME_AZ -> rawAppList.sortedBy { it.appName.lowercase() }
            SortOrder.PACKAGE_NAME -> rawAppList.sortedBy { it.packageName }
            SortOrder.USAGE_MOST_USED -> rawAppList.sortedByDescending { it.usage1DayMillis } 
        }
    }

    private fun formatMillis(millis: Long?): String {
        if (millis == null || millis == 0L) return "0m"
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true 

            val app = getApplication<Application>()
            val packageManager = app.packageManager
            val usageStatsManager = app.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            
            val stats1Day = if (_hasUsagePermission.value) usageStatsManager.queryAndAggregateUsageStats(now - (1000L * 60 * 60 * 24), now) else emptyMap()
            val stats3Days = if (_hasUsagePermission.value) usageStatsManager.queryAndAggregateUsageStats(now - (1000L * 60 * 60 * 24 * 3), now) else emptyMap()
            val stats1Week = if (_hasUsagePermission.value) usageStatsManager.queryAndAggregateUsageStats(now - (1000L * 60 * 60 * 24 * 7), now) else emptyMap()
            val stats1Month = if (_hasUsagePermission.value) usageStatsManager.queryAndAggregateUsageStats(now - (1000L * 60 * 60 * 24 * 30), now) else emptyMap()

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
                    3 -> RiskLevel.HIGH; 1, 2 -> RiskLevel.MEDIUM; else -> RiskLevel.LOW
                }

                // NEW: Calculate raw millis for all four timeframes
                val raw1Day = stats1Day[pack.packageName]?.totalTimeInForeground ?: 0L
                val raw3Days = stats3Days[pack.packageName]?.totalTimeInForeground ?: 0L
                val raw1Week = stats1Week[pack.packageName]?.totalTimeInForeground ?: 0L
                val raw1Month = stats1Month[pack.packageName]?.totalTimeInForeground ?: 0L

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
                        usage1Day = formatMillis(raw1Day),
                        usage3Days = formatMillis(raw3Days),
                        usage1Week = formatMillis(raw1Week),
                        usage1Month = formatMillis(raw1Month),
                        usage1DayMillis = raw1Day,
                        usage3DaysMillis = raw3Days, // Pass them in!
                        usage1WeekMillis = raw1Week,
                        usage1MonthMillis = raw1Month,
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