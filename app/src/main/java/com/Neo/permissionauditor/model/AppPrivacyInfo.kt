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
    
    val totalPermissionsRequested: Int,
    
    val usage1Day: String,
    val usage3Days: String,
    val usage1Week: String,
    val usage1Month: String,
    val usage1DayMillis: Long, // NEW: Raw number so we can sort highest to lowest!
    
    val riskLevel: RiskLevel
)
