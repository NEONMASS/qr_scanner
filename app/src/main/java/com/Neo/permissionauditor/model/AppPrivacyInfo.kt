package com.Neo.permissionauditor.model // <-- Safely lowercase

enum class RiskLevel { LOW, MEDIUM, HIGH }

data class AppPrivacyInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    
    val hasCameraAccess: Boolean,
    val isCameraGranted: Boolean, // NEW: Is it actually enabled?
    
    val hasLocationAccess: Boolean,
    val isLocationGranted: Boolean, // NEW: Is it actually enabled?
    
    val hasMicrophoneAccess: Boolean,
    val isMicrophoneGranted: Boolean, // NEW: Is it actually enabled?
    
    val riskLevel: RiskLevel
)
