package com.Neo.permissionauditor.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Neo.permissionauditor.model.AppPrivacyInfo
import com.Neo.permissionauditor.model.RiskLevel

@Composable
fun AppRow(appInfo: AppPrivacyInfo, isGridMode: Boolean = false) { // NEW: Grid mode flag
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    val openSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${appInfo.packageName}")
        }
        context.startActivity(intent)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(appInfo.appName) },
            text = {
                Column {
                    Text(
                        text = "Total Permissions: ${appInfo.totalPermissionsRequested}", 
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sensitive Status:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (appInfo.hasCameraAccess) {
                        Text("Camera: ${if (appInfo.isCameraGranted) " Enabled" else " Disabled"}")
                    }
                    if (appInfo.hasMicrophoneAccess) {
                        Text("Microphone: ${if (appInfo.isMicrophoneGranted) " Enabled" else " Disabled"}")
                    }
                    if (appInfo.hasLocationAccess) {
                        Text("Location: ${if (appInfo.isLocationGranted) " Enabled" else " Disabled"}")
                    }
                    if (!appInfo.hasCameraAccess && !appInfo.hasMicrophoneAccess && !appInfo.hasLocationAccess) {
                        Text("No sensitive permissions requested.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    openSettings()
                }) { Text("Edit") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // NEW: If we are in grid mode, force every card to be exactly 140dp tall!
            .then(if (isGridMode) Modifier.height(140.dp) else Modifier.wrapContentHeight())
            .clickable { showDialog = true }, 
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Forces it to fill the 140dp height in Grid mode
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = openSettings,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val riskColor = when (appInfo.riskLevel) {
                RiskLevel.HIGH -> Color.Red
                RiskLevel.MEDIUM -> Color(0xFFFFA500)
                RiskLevel.LOW -> Color.Green
            }
            Text(
                text = "${appInfo.riskLevel.name} RISK",
                color = riskColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // NEW: Push the badges perfectly to the bottom of the card if in Grid Mode
            if (isGridMode) {
                Spacer(modifier = Modifier.weight(1f)) 
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // NEW: Linear Horizontal Layout for badges!
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (appInfo.hasCameraAccess) PermissionBadge("Cam", appInfo.isCameraGranted)
                if (appInfo.hasMicrophoneAccess) PermissionBadge("Mic", appInfo.isMicrophoneGranted)
                if (appInfo.hasLocationAccess) PermissionBadge("Loc", appInfo.isLocationGranted)
            }
        }
    }
}

@Composable
fun PermissionBadge(label: String, isGranted: Boolean) {
    val bgColor = if (isGranted) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
    val textColor = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer

    Surface(color = bgColor, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
