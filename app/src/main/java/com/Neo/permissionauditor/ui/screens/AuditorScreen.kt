package com.Neo.permissionauditor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Sort Apps By",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Risk Level (Highest First)") },
                    selected = currentSort == SortOrder.RISK,
                    onClick = {
                        viewModel.setSortOrder(SortOrder.RISK)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Company Grouping (Grid Layout)") },
                    selected = currentSort == SortOrder.PACKAGE_NAME,
                    onClick = {
                        viewModel.setSortOrder(SortOrder.PACKAGE_NAME)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("App Name (A-Z)") },
                    selected = currentSort == SortOrder.APP_NAME,
                    onClick = {
                        viewModel.setSortOrder(SortOrder.APP_NAME)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search apps...") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                isSearchActive = false
                                viewModel.updateSearchQuery("") 
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close Search")
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Text")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                } else {
                    TopAppBar(
                        title = { 
                            Column {
                                Text("Permission Auditor v2")
                                if (!isLoading) {
                                    Text(
                                        text = "${filteredApps.size} apps found",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Open Search")
                            }
                            Switch(
                                checked = showSystemApps,
                                onCheckedChange = { viewModel.toggleSystemApps(it) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // NEW: Conditional UI rendering!
                    if (currentSort == SortOrder.PACKAGE_NAME) {
                        // 2-COLUMN GRID VIEW (Company Grouping)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            groupedApps.forEach { (companyName, companyApps) ->
                                item(span = { GridItemSpan(2) }) {
                                    Text(
                                        text = companyName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                    )
                                }
                                gridItems(companyApps) { appInfo ->
                                    AppRow(appInfo = appInfo, isGridMode = true) // Pass flag to lock size
                                }
                            }
                        }
                    } else {
                        // STANDARD LIST VIEW (Risk & App Name Sorting)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            lazyItems(filteredApps) { appInfo ->
                                AppRow(appInfo = appInfo, isGridMode = false) // Standard height
                            }
                        }
                    }
                }
            }
        }
    }
}