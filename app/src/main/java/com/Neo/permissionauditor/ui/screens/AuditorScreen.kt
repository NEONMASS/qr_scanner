package com.yourname.permissionauditor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yourname.permissionauditor.ui.components.AppRow
import com.yourname.permissionauditor.viewmodel.AuditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditorScreen(viewModel: AuditorViewModel) {
    val context = LocalContext.current
    
    // Observe the state from the ViewModel
    val appList by viewModel.appList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Trigger the scan exactly once when the screen loads
    LaunchedEffect(Unit) {
        viewModel.scanDevicePermissions(context, showSystemApps = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Auditor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Show a loading spinner while querying the OS
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // Display the high-performance scrolling list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(appList) { appInfo ->
                        AppRow(appInfo = appInfo)
                    }
                }
            }
        }
    }
}