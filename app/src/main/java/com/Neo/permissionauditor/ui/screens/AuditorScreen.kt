package com.Neo.permissionauditor.ui.screens // <-- Fixed lowercase 'p'

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Neo.permissionauditor.ui.components.AppRow
import com.Neo.permissionauditor.viewmodel.AuditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditorScreen(viewModel: AuditorViewModel = viewModel()) {

    val apps by viewModel.installedApps.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Auditor v2") }, // <-- Added v2 stamp
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
            // THE UPGRADED TOGGLE UI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    // Make the entire card a giant clickable button
                    .clickable { viewModel.toggleSystemApps(!showSystemApps) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show System Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Switch(
                        checked = showSystemApps,
                        onCheckedChange = { viewModel.toggleSystemApps(it) }
                    )
                }
            }

            // SMART LOADING SPINNER
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
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
