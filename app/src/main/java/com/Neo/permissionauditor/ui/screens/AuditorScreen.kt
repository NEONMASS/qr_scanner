package com.Neo.permissionauditor.ui.screens

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

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCompany by remember { mutableStateOf<String?>(null) }

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

    // Handle physical back button
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
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    } else {
                        Text(selectedCompany ?: "Permission Auditor")
                    }
                },
                navigationIcon = {
                    // Only show the back arrow if we are searching or inside a company view!
                    if (isSearchActive || selectedCompany != null) {
                        IconButton(onClick = {
                            if (isSearchActive) { isSearchActive = false; viewModel.updateSearchQuery("") }
                            else if (selectedCompany != null) { selectedCompany = null }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Default.Search, null) }
                        Switch(checked = showSystemApps, onCheckedChange = { viewModel.toggleSystemApps(it) }, modifier = Modifier.padding(end = 8.dp))
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        // NEW: The Bottom Navigation Bar!
        bottomBar = {
            // Auto-hide the bottom bar if the user is searching or viewing a specific company's apps
            if (!isSearchActive && selectedCompany == null) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Warning, contentDescription = "Risk Level") },
                        label = { Text("Risk") },
                        selected = currentSort == SortOrder.RISK,
                        onClick = { 
                            viewModel.setSortOrder(SortOrder.RISK)
                            selectedCompany = null 
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Build, contentDescription = "Company") },
                        label = { Text("Company") },
                        selected = currentSort == SortOrder.PACKAGE_NAME,
                        onClick = { viewModel.setSortOrder(SortOrder.PACKAGE_NAME) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "A-Z") },
                        label = { Text("A-Z") },
                        selected = currentSort == SortOrder.APP_NAME,
                        onClick = { 
                            viewModel.setSortOrder(SortOrder.APP_NAME)
                            selectedCompany = null 
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                            gridItems(groupedApps.keys.toList()) { company ->
                                CompanyCard(company, groupedApps[company]?.size ?: 0) { selectedCompany = company }
                            }
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
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(text = name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
            Text(text = "$count Apps", style = MaterialTheme.typography.bodySmall)
        }
    }
}
