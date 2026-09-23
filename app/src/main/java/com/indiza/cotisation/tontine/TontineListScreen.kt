package com.indiza.cotisation.tontine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // <-- Très important !
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TontineListScreen(
    viewModel: TontineListViewModel = hiltViewModel(),
    onReunionClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }

    val tontines by viewModel.tontines.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var tontineToDelete by remember { mutableStateOf<Long?>(null) }
    if (tontineToDelete != null) {
        AlertDialog(
            onDismissRequest = { tontineToDelete = null },
            title = { Text("Supprimer la tontine ?") },
            text = { Text("Cette action est irréversible. La tontine doit être vide de membres et de caisses.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTontine(tontineToDelete!!)
                    tontineToDelete = null
                }) { Text("Confirmer", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { tontineToDelete = null }) { Text("Annuler") }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.deleteError.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF333333), // Gris très foncé/noir
                    contentColor = Color.White,
                    dismissActionContentColor = NeonCyan // Ton bleu d'accentuation
                )
            }
        },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Mes Réunions", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onProfileClick) {
                            Icon(Icons.Default.Person, contentDescription = "Profil", tint = NeonCyan)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshTontines() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Trier")
                        }
                        // Menu de tri
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Plus récentes") },
                                onClick = { viewModel.onSortOrderChange(TontineSortOrder.DATE_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Nom (A-Z)") },
                                onClick = { viewModel.onSortOrderChange(TontineSortOrder.NAME_ASC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Nombre de caisses") },
                                onClick = { viewModel.onSortOrderChange(TontineSortOrder.CAISSE_COUNT); showSortMenu = false }
                            )
                        }
                    }
                )

                // Barre de recherche
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Chercher une réunion...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                lastSyncTime?.let { time ->
                    val dateStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(time))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Synchronisé à $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                containerColor = NeonCyan, // Utilisation de votre couleur d'accentuation
                contentColor = Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = "CRÉER UNE RÉUNION",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        if (tontines.isEmpty()) {
            EmptyTontineState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = tontines, key = { it.tontine.id }) { item ->
                    TontineItem(
                        tontineWithCaisses = item,
                        onClick = { onReunionClick(item.tontine.id) },
                        onEditClick = { onEditClick(item.tontine.id) }, // <--- Connecté
                        onDeleteClick = { id -> tontineToDelete = id } // <--- Connecté
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTontineState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text("Aucune réunion enregistrée", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Text("Cliquez sur + pour créer votre première tontine", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

