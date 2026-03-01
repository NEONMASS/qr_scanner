package com.Neo.permissionauditor.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Neo.permissionauditor.ui.components.AppRow
import com.Neo.permissionauditor.viewmodel.AuditorViewModel
import com.Neo.permissionauditor.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditorScreen(viewModel: AuditorViewModel = viewModel()) {

    val apps by viewModel.installedApps.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCompany by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    val filteredApps = apps.filter { 
        it.appName.contains(searchQuery, ignoreCase = true) || 
        it.packageName.contains(searchQuery, ignoreCase = true) 
    }

    val groupedApps = remember(filteredApps) {
        filteredApps.groupBy { appInfo ->
            val parts = appInfo.packageName.split(".")
            if (parts.size >= 2) "${parts[0]}.${parts[1]}" else appInfo.packageName
        }.toSortedMap()
    }

    BackHandler(enabled = selectedCompany != null) {
        selectedCompany = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    } else {
                        Text(selectedCompany ?: "Permission Auditor")
                    }
                },
                navigationIcon = {
                    if (isSearchActive || selectedCompany != null) {
                        IconButton(onClick = {
                            if (isSearchActive) { isSearchActive = false; viewModel.updateSearchQuery("") }
                            else if (selectedCompany != null) { selectedCompany = null }
                        }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("Risk (High to Low)") }, onClick = { viewModel.setSortOrder(SortOrder.RISK_HIGH_FIRST); selectedCompany = null; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Risk (Low to High)") }, onClick = { viewModel.setSortOrder(SortOrder.RISK_LOW_FIRST); selectedCompany = null; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Name (A to Z)") }, onClick = { viewModel.setSortOrder(SortOrder.APP_NAME_AZ); selectedCompany = null; showSortMenu = false })
                            }
                        }
                        IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Default.Search, null) }
                        Switch(checked = showSystemApps, onCheckedChange = { viewModel.toggleSystemApps(it) }, modifier = Modifier.padding(end = 8.dp))
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            if (!isSearchActive && selectedCompany == null) {
                NavigationBar {
                    NavigationBarItem(icon = { Icon(Icons.Default.List, null) }, label = { Text("All Apps") }, selected = currentSort != SortOrder.PACKAGE_NAME, onClick = { if (currentSort == SortOrder.PACKAGE_NAME) viewModel.setSortOrder(SortOrder.RISK_HIGH_FIRST); selectedCompany = null })
                    NavigationBarItem(icon = { Icon(Icons.Default.Build, null) }, label = { Text("Companies") }, selected = currentSort == SortOrder.PACKAGE_NAME, onClick = { viewModel.setSortOrder(SortOrder.PACKAGE_NAME) })
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            
            // NEW: The Permission Request Banner
            if (!hasUsagePermission && !isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { 
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Usage Access Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Tap here to grant permission to view screen time stats.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                
                // Refresh button to check if they granted it after returning from settings
                TextButton(
                    onClick = { viewModel.checkPermissionAndLoadApps() }, 
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("I granted it, Refresh!") }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                when {
                    currentSort == SortOrder.PACKAGE_NAME && selectedCompany != null -> {
                        val companyApps = groupedApps[selectedCompany] ?: emptyList()
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            lazyItems(companyApps) { AppRow(it, isGridMode = false) }
                        }
                    }
                    currentSort == SortOrder.PACKAGE_NAME -> {
                        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            gridItems(groupedApps.keys.toList()) { company -> CompanyCard(company, groupedApps[company]?.size ?: 0) { selectedCompany = company } }
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            lazyItems(filteredApps) { AppRow(it, isGridMode = false) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyCard(name: String, count: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(120.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(text = name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
            Text(text = "$count Apps", style = MaterialTheme.typography.bodySmall)
        }
    }
}
