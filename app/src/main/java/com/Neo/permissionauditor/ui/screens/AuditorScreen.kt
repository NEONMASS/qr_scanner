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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Neo.permissionauditor.ui.components.AppRow
import com.Neo.permissionauditor.model.AppPrivacyInfo
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

    BackHandler(enabled = selectedCompany != null) { selectedCompany = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery, onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search...") },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            singleLine = true
                        )
                    } else {
                        Text(selectedCompany ?: "Permission Auditor Pro")
                    }
                },
                navigationIcon = {
                    if (isSearchActive || selectedCompany != null) {
                        IconButton(onClick = {
                            if (isSearchActive) { isSearchActive = false; viewModel.updateSearchQuery("") }
                            else if (selectedCompany != null) { selectedCompany = null }
                        }) { Icon(Icons.Default.ArrowBack, "Back") }
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
                    NavigationBarItem(icon = { Icon(Icons.Default.List, null) }, label = { Text("Apps") }, selected = currentSort != SortOrder.PACKAGE_NAME && currentSort != SortOrder.USAGE_MOST_USED, onClick = { if (currentSort == SortOrder.PACKAGE_NAME || currentSort == SortOrder.USAGE_MOST_USED) viewModel.setSortOrder(SortOrder.RISK_HIGH_FIRST); selectedCompany = null })
                    NavigationBarItem(icon = { Icon(Icons.Default.Build, null) }, label = { Text("Companies") }, selected = currentSort == SortOrder.PACKAGE_NAME, onClick = { viewModel.setSortOrder(SortOrder.PACKAGE_NAME) })
                    NavigationBarItem(icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Usage") }, selected = currentSort == SortOrder.USAGE_MOST_USED, onClick = { viewModel.setSortOrder(SortOrder.USAGE_MOST_USED); selectedCompany = null })
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            
            if (!hasUsagePermission && !isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Usage Access Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Tap here to grant permission for screen time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                TextButton(onClick = { viewModel.checkPermissionAndLoadApps() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("I granted it, Refresh!") }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                when {
                    currentSort == SortOrder.USAGE_MOST_USED -> {
                        val activeApps = filteredApps.filter { it.usage1DayMillis > 0 }
                        // NEW: Calculate the maximum values so we know what 100% width is!
                        val max1Day = activeApps.maxOfOrNull { it.usage1DayMillis }?.coerceAtLeast(1L) ?: 1L
                        val max3Days = activeApps.maxOfOrNull { it.usage3DaysMillis }?.coerceAtLeast(1L) ?: 1L
                        val max1Week = activeApps.maxOfOrNull { it.usage1WeekMillis }?.coerceAtLeast(1L) ?: 1L
                        val max1Month = activeApps.maxOfOrNull { it.usage1MonthMillis }?.coerceAtLeast(1L) ?: 1L

                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Pass the max values into the row to draw the graphs
                            lazyItems(activeApps) { app -> 
                                UsageRow(app, max1Day, max3Days, max1Week, max1Month) 
                            }
                        }
                    }
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

// NEW: The Graph-Based Usage Row
@Composable
fun UsageRow(appInfo: AppPrivacyInfo, max1Day: Long, max3Days: Long, max1Week: Long, max1Month: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = appInfo.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Draw 4 distinct bar graphs!
            UsageBarGraph("Past 24 Hours", appInfo.usage1Day, appInfo.usage1DayMillis, max1Day, Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(8.dp))
            UsageBarGraph("Past 3 Days", appInfo.usage3Days, appInfo.usage3DaysMillis, max3Days, Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(8.dp))
            UsageBarGraph("Past Week", appInfo.usage1Week, appInfo.usage1WeekMillis, max1Week, Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(8.dp))
            UsageBarGraph("Past Month", appInfo.usage1Month, appInfo.usage1MonthMillis, max1Month, Color(0xFFE91E63))
        }
    }
}

// NEW: A reusable composable to draw a clean, rounded progress bar
@Composable
fun UsageBarGraph(label: String, timeText: String, millis: Long, maxMillis: Long, barColor: Color) {
    // Prevent divide by zero, calculate percentage
    val progress = if (maxMillis > 0) (millis.toFloat() / maxMillis.toFloat()).coerceIn(0f, 1f) else 0f
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Text(text = timeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun CompanyCard(name: String, count: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(120.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(text = name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
            Text(text = "$count Apps", style = MaterialTheme.typography.bodySmall)
        }
    }
}