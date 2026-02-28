package com.yourname.permissionauditor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.permissionauditor.model.AppPrivacyInfo
import com.yourname.permissionauditor.model.RiskLevel

@Composable
fun AppRow(appInfo: AppPrivacyInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // App Name and Risk Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Color code the risk text
                val riskColor = when (appInfo.riskLevel) {
                    RiskLevel.HIGH -> Color.Red
                    RiskLevel.MEDIUM -> Color(0xFFFFA500) // Orange
                    RiskLevel.LOW -> Color.Green
                }
                
                Text(
                    text = appInfo.riskLevel.name,
                    color = riskColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Package name for technical context
            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Permission Indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (appInfo.hasCameraAccess) PermissionBadge("Camera")
                if (appInfo.hasMicrophoneAccess) PermissionBadge("Microphone")
                if (appInfo.hasLocationAccess) PermissionBadge("Location")
            }
        }
    }
}

@Composable
fun PermissionBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}