package com.Neo.permissionauditor.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.Neo.permissionauditor.ui.components.AppRow
import com.Neo.permissionauditor.model.AppPrivacyInfo
import com.Neo.permissionauditor.utils.ExportUtils
import com.Neo.permissionauditor.viewmodel.AuditorViewModel
import com.Neo.permissionauditor.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// NEW: Accept the onThemeChange callback from MainActivity!
fun AuditorScreen(viewModel: AuditorViewModel = viewModel(), onThemeChange: (String) -> Unit = {}) {

    val apps by viewModel.installedApps.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCompany by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch { ExportUtils.exportToCsv(context, uri, filteredApps); Toast.makeText(context, "Excel File Saved!", Toast.LENGTH_SHORT).show() }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            scope.launch { ExportUtils.exportToPdf(context, uri, filteredApps); Toast.makeText(context, "PDF Report Saved!", Toast.LENGTH_SHORT).show() }
        }
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
                        // Title changes depending on the tab
                        Text(if (currentSort == SortOrder.SETTINGS) "Settings" else (selectedCompany ?: "Permission Auditor PRO"))
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
                    // Hide search and sort on the Settings tab!
                    if (!isSearchActive && currentSort != SortOrder.SETTINGS) {
                        Box {
                            IconButton(onClick = { showExportMenu = true }) { Icon(Icons.Default.Share, "Export") }
                            DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                                DropdownMenuItem(text = { Text("Export to Excel (.csv)") }, onClick = { showExportMenu = false; csvLauncher.launch("Auditor_Report.csv") })
                                DropdownMenuItem(text = { Text("Export to PDF") }, onClick = { showExportMenu = false; pdfLauncher.launch("Auditor_Report.pdf") })
                            }
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) { Icon(Icons.Default.MoreVert, "Sort Options") }
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
                    NavigationBarItem(icon = { Icon(Icons.Default.List, null) }, label = { Text("Apps") }, selected = currentSort != SortOrder.PACKAGE_NAME && currentSort != SortOrder.USAGE_MOST_USED && currentSort != SortOrder.SETTINGS, onClick = { if (currentSort != SortOrder.RISK_HIGH_FIRST) viewModel.setSortOrder(SortOrder.RISK_HIGH_FIRST); selectedCompany = null })
                    NavigationBarItem(icon = { Icon(Icons.Default.Build, null) }, label = { Text("Companies") }, selected = currentSort == SortOrder.PACKAGE_NAME, onClick = { viewModel.setSortOrder(SortOrder.PACKAGE_NAME) })
                    NavigationBarItem(icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Usage") }, selected = currentSort == SortOrder.USAGE_MOST_USED, onClick = { viewModel.setSortOrder(SortOrder.USAGE_MOST_USED); selectedCompany = null })
                    // NEW: Settings Tab
                    NavigationBarItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, selected = currentSort == SortOrder.SETTINGS, onClick = { viewModel.setSortOrder(SortOrder.SETTINGS); selectedCompany = null })
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            
            if (!hasUsagePermission && !isLoading && currentSort != SortOrder.SETTINGS) {
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
                    // NEW: Render the Settings View
                    currentSort == SortOrder.SETTINGS -> {
                        SettingsView(onThemeChange = onThemeChange)
                    }
                    currentSort == SortOrder.USAGE_MOST_USED -> {
                        val activeApps = filteredApps.filter { it.usage1DayMillis > 0 }
                        val max1Day = activeApps.maxOfOrNull { it.usage1DayMillis }?.coerceAtLeast(1L) ?: 1L
                        val max3Days = activeApps.maxOfOrNull { it.usage3DaysMillis }?.coerceAtLeast(1L) ?: 1L
                        val max1Week = activeApps.maxOfOrNull { it.usage1WeekMillis }?.coerceAtLeast(1L) ?: 1L
                        val max1Month = activeApps.maxOfOrNull { it.usage1MonthMillis }?.coerceAtLeast(1L) ?: 1L

                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            lazyItems(activeApps) { app -> UsageRow(app, max1Day, max3Days, max1Week, max1Month) }
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

// NEW: A beautifully structured Settings Menu!
@Composable
fun SettingsView(onThemeChange: (String) -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("AuditorPrefs", Context.MODE_PRIVATE)

    // Default to our new auto_time feature!
    var currentTheme by remember { mutableStateOf(sharedPrefs.getString("theme", "auto_time") ?: "auto_time") }
    var hasPin by remember { mutableStateOf(!sharedPrefs.getString("app_pin", "").isNullOrEmpty()) }
    var useBiometrics by remember { mutableStateOf(sharedPrefs.getBoolean("use_biometrics", false)) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember { biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS }

    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false; pinInput = "" },
            title = { Text("Set 4-Digit PIN") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4) pinInput = it.filter { char -> char.isDigit() } },
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.length == 4) {
                        sharedPrefs.edit().putString("app_pin", pinInput).apply()
                        hasPin = true
                        showPinSetupDialog = false
                        pinInput = ""
                        Toast.makeText(context, "App Lock Enabled!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false; pinInput = "" }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // --- THEME SECTION ---
        Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        // NEW: We added "Auto (Clock)" to the choices and arranged them nicely
        Column(modifier = Modifier.fillMaxWidth()) {
            val themes = listOf(
                "auto_time" to "Auto (Day/Night Clock)",
                "system" to "System Default",
                "light" to "Force Light Mode", 
                "dark" to "Force Dark Mode"
            )
            themes.forEach { (key, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { 
                    currentTheme = key
                    sharedPrefs.edit().putString("theme", key).apply()
                    onThemeChange(key)
                }.padding(vertical = 4.dp)) {
                    RadioButton(selected = currentTheme == key, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(Modifier.height(32.dp))

        // --- SECURITY SECTION ---
        Text("Security", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("App Lock (PIN)", fontWeight = FontWeight.Medium)
                Text("Require a PIN to open Auditor PRO", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = hasPin, onCheckedChange = {
                if (it) {
                    showPinSetupDialog = true
                } else {
                    sharedPrefs.edit().remove("app_pin").putBoolean("use_biometrics", false).apply()
                    hasPin = false
                    useBiometrics = false
                }
            })
        }

        if (hasPin) {
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Biometric Unlock", fontWeight = FontWeight.Medium)
                    if (canAuthenticate) {
                        Text("Use fingerprint or face recognition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("⚠️ Enable a fingerprint or screen lock in your phone's Android Settings first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Switch(
                    checked = useBiometrics && canAuthenticate, 
                    onCheckedChange = {
                        useBiometrics = it
                        sharedPrefs.edit().putBoolean("use_biometrics", it).apply()
                    },
                    enabled = canAuthenticate 
                )
            }
        }
    }
}

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

@Composable
fun UsageBarGraph(label: String, timeText: String, millis: Long, maxMillis: Long, barColor: Color) {
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