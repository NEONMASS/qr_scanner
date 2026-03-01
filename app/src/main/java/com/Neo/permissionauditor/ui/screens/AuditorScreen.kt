package com.Neo.permissionauditor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Neo.permissionauditor.ui.components.AppRow
import com.Neo.permissionauditor.viewmodel.AuditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditorScreen(viewModel: AuditorViewModel = viewModel()) {
    
    // Observe state from the ViewModel
    val apps by viewModel.installedApps.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Auditor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // The Toggle Switch UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show System Apps",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = showSystemApps,
                    onCheckedChange = { isChecked -> 
                        viewModel.toggleSystemApps(isChecked) 
                    }
                )
            }

            // Show a loading spinner if the app list is empty (still loading)
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // The high-performance list of apps
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apps) { appInfo ->
                        AppRow(appInfo = appInfo)
                    }
                }
            }
        }
    }
}
