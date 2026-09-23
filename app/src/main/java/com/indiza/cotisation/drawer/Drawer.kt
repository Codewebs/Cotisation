package com.indiza.cotisation.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indiza.cotisation.Cotisard
import com.indiza.cotisation.caisses.CaisseDetailScreen
import com.indiza.cotisation.sync.ConnectivityObserver
import com.indiza.cotisation.tontine.CreateTontineScreen
import com.indiza.cotisation.tontine.EditTontineScreen
import com.indiza.cotisation.tontine.TontineDetailScreen
import com.indiza.cotisation.tontine.TontineListScreen
import com.indiza.cotisation.tontine.TontineRepository
import com.indiza.cotisation.users.AuthScreen
import com.indiza.cotisation.users.ProfileScreen
import com.indiza.cotisation.users.SearchTontineScreen
import com.indiza.cotisation.users.SearchTontineViewModel
import com.indiza.cotisation.users.SessionViewModel
import kotlinx.coroutines.launch

enum class Screen {
    HOME, CREATE_TONTINE, LIST_TONTINE, SEARCH_TONTINE, AUTH, DETAILS_TONTINE, EDIT_TONTINE, DETAILS_CAISSE, PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScreen(connectivityObserver: ConnectivityObserver) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTontineId by remember { mutableStateOf<Long?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedCaisseId by remember { mutableStateOf<Long?>(null) }

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val searchViewModel: SearchTontineViewModel = hiltViewModel()
    val sessionViewModel: SessionViewModel = hiltViewModel()

    val syncEnabled by sessionViewModel.syncEnabled.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEEEEEE))
                    .padding(16.dp)
            ) {

                Text("Menu", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(24.dp))
                DrawerItem("Rechercher", Icons.Default.Search) {
                    currentScreen = Screen.SEARCH_TONTINE
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Cotisations", Icons.Default.List) {
                    currentScreen = Screen.HOME
                    scope.launch { drawerState.close() }
                }

                // Action pour ouvrir la liste des réunions
                DrawerItem("Reunions", Icons.Default.Groups) {
                    currentScreen = Screen.LIST_TONTINE
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Statistiques", Icons.Default.ShowChart) { /* ... */ }
                DrawerItem("Paramètres", Icons.Default.Settings) { /* ... */ }

                SyncControlRow(
                    syncEnabled = syncEnabled,
                    onToggle = { newValue ->
                        sessionViewModel.toggleSync(newValue)
                    }
                )
            }
        }
    ) {
        // Switch pour choisir l'écran à afficher
        when (currentScreen) {
            Screen.SEARCH_TONTINE-> {
                SearchTontineScreen(
                    viewModel = searchViewModel,
                    sessionViewModel = sessionViewModel,
                    notifViewModel = hiltViewModel(),
                    adminViewModel = hiltViewModel(),
                    onBack = { currentScreen = Screen.HOME },
                    onCreateTontineWhenAuthenticated = {
                        currentScreen = Screen.CREATE_TONTINE
                    },
                    onNavigateToAuth = {
                        currentScreen = Screen.AUTH
                    },
                    onJoinTontine = { tontine ->
                        // Logique pour rejoindre
                    },
                    onNavigateToList = {
                        currentScreen = Screen.LIST_TONTINE
                    },
                    onNavigateToProfile = {
                        currentScreen = Screen.PROFILE
                    },
                    onNavigateToReceipt = { /* Pas géré dans Drawer pour le moment */ },
                    onLogout = {
                        sessionViewModel.logout()
                        currentScreen = Screen.HOME
                    }
                )
            }
            Screen.DETAILS_TONTINE -> {

            }
            Screen.DETAILS_CAISSE -> {
                selectedCaisseId?.let { id ->
                    CaisseDetailScreen(
                        caisseId = id,
                        cycleId = id,
                        onBack = { currentScreen = Screen.DETAILS_TONTINE },
                        navController = TODO(),
                        viewModel = TODO()
                    )
                }
            }

            Screen.CREATE_TONTINE -> {
                CreateTontineScreen(
                    onCancel = { currentScreen = Screen.HOME },
                    onSuccess = { currentScreen = Screen.LIST_TONTINE },
                    viewModel = TODO(),
                    onBack = TODO()
                )
            }
            Screen.HOME -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Gestion Cotisation") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Nouvelle réunion (Tontine)") },
                                    leadingIcon = { Icon(Icons.Default.Groups, null) },
                                    onClick = {
                                        showMenu = false
                                        currentScreen = Screen.CREATE_TONTINE // <-- Navigation ici
                                    }
                                )
                                // Ajoutez d'autres actions ici
                            }

                            FloatingActionButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.Add, contentDescription = "Ajouter")
                            }
                        }
                    }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                        ActuScreen(connectivityObserver)
                    }
                }
            }
            Screen.LIST_TONTINE -> {
                TontineListScreen(
                    onReunionClick = { id ->
                        // 1. On stocke l'ID de la tontine cliquée
                        selectedTontineId = id
                        // 2. On change l'écran vers les détails
                        currentScreen = Screen.DETAILS_TONTINE
                    },
                    onEditClick = { id -> // <--- Nouvelle action
                        selectedTontineId = id
                        currentScreen = Screen.EDIT_TONTINE
                    },
                    onAddClick = { currentScreen = Screen.CREATE_TONTINE },
                    onProfileClick = { currentScreen = Screen.PROFILE }
                )
            }

            Screen.EDIT_TONTINE -> {
                selectedTontineId?.let { id ->
                    EditTontineScreen(
                        tontineId = id,
                        onBack = { currentScreen = Screen.LIST_TONTINE }
                    )
                }
            }
            Screen.AUTH -> {
                AuthScreen(
                    onAuthSuccess = {
                        currentScreen = Screen.HOME
                    }
                )
            }
            Screen.PROFILE -> {
                ProfileScreen(
                    onBack = { currentScreen = Screen.HOME },
                    onLogout = {
                        sessionViewModel.logout()
                        currentScreen = Screen.HOME
                    },
                    onTontineClick = { tontineId ->
                        selectedTontineId = tontineId
                        currentScreen = Screen.DETAILS_TONTINE
                    }
                )
            }

        }
    }
}


@Composable
fun DrawerItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit // Ajout du callback
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(onClick = onClick), // Déclenche l'action
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}
