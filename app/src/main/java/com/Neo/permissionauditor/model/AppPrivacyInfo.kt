package com.Neo.permissionauditor.model

enum class RiskLevel { LOW, MEDIUM, HIGH }

data class AppPrivacyInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    
    val hasCameraAccess: Boolean,
    val isCameraGranted: Boolean,
    
    val hasLocationAccess: Boolean,
    val isLocationGranted: Boolean,
    
    val hasMicrophoneAccess: Boolean,
    val isMicrophoneGranted: Boolean,
    
    val totalPermissionsRequested: Int, // NEW: Track total permissions
    
    val riskLevel: RiskLevel
)
