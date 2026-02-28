package com.Neo.permissionauditor.model

data class AppPrivacyInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val hasCameraAccess: Boolean,
    val hasLocationAccess: Boolean,
    val hasMicrophoneAccess: Boolean,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    HIGH, MEDIUM, LOW
}