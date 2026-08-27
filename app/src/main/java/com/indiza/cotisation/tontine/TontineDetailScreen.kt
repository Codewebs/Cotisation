package com.indiza.cotisation.tontine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.CosmicBackground
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.indiza.cotisation.caisses.CaisseConfigScreen
import com.indiza.cotisation.caisses.CaisseType
import com.indiza.cotisation.tontinemembers.AddMemberDialog
import com.indiza.cotisation.tontinemembers.EditMemberDialog
import com.indiza.cotisation.tontinemembers.MemberItem
import com.indiza.cotisation.tontinemembers.MemberUiModel
import com.indiza.cotisation.users.Permission
import com.indiza.cotisation.users.Role

@Composable
fun TontineDetailScreen(
    tontineId: Long,
    viewModel: TontineDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCaisseClick: (Long, Long) -> Unit
) {
    LaunchedEffect(tontineId) { viewModel.loadTontine(tontineId) }

    val state by viewModel.state.collectAsState()
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var selectedMemberForEdit by remember { mutableStateOf<MemberUiModel?>(null) }
    var showTypeSelection by remember { mutableStateOf(false) }
    var selectedTypeForConfig by remember { mutableStateOf<CaisseType?>(null) }

    if (showTypeSelection) {
        TypeSelectionDialog(
            onTypeSelected = { type ->
                selectedTypeForConfig = type
                showTypeSelection = false
            },
            onDismiss = { showTypeSelection = false }
        )
    }

    if (selectedTypeForConfig != null) {
        Dialog(
            onDismissRequest = { selectedTypeForConfig = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CaisseConfigScreen(
                typeInitial = selectedTypeForConfig!!,
                onSave = { draft ->
                    // Appel au repository via le viewModel pour insérer la caisse
                    // Tu peux ajouter une fonction 'addCaisseToTontine' dans TontineDetailViewModel
                    viewModel.addCaisseToTontine(tontineId, draft)
                    selectedTypeForConfig = null
                },
                onCancel = { selectedTypeForConfig = null }
            )
        }
    }

    selectedMemberForEdit?.let { member ->
        EditMemberDialog(
            member = member,
            onDismiss = { selectedMemberForEdit = null },
            onConfirm = { n, p, r ->
                viewModel.updateMember(member.id, n, p, r)
                selectedMemberForEdit = null
            },
            onMapRequest = {
                /* On gérera le mapping plus tard comme convenu */
                selectedMemberForEdit = null
            }
        )
    }

    CosmicBackground {
        when (val uiState = state) {
            is TontineDetailUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            is TontineDetailUiState.Success -> {
                val tontine = uiState.tontineWithCaisses.tontine
                val caisses = uiState.tontineWithCaisses.caisses
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = { showAddMemberDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp),
                            icon = {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Ajouter Membre",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding) // Utilisation du padding du Scaffold
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        if (showAddMemberDialog) {
                            AddMemberDialog(
                                onDismiss = { showAddMemberDialog = false },
                                onConfirm = { n, p, r ->
                                    viewModel.addMember(tontine.id, n, p, r)
                                    showAddMemberDialog = false
                                }
                            )
                        }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // 2. Libellé de la Tontine
                            Text(
                                text = tontine.libelle.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 3. Badge du Rôle
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = uiState.role.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    QuickActionsRow(uiState.role)
                    Spacer(modifier = Modifier.height(24.dp))
                    // Liste des Caisses
                    Text(
                        "CAISSES ACTIVES",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (uiState.role.permissions.contains(Permission.EDIT_TONTINE_RULES)) {
                        TextButton(onClick = { showTypeSelection = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nouvelle Caisse", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    uiState.caissesWithCycles.forEach { item ->
                        val solde = viewModel.getCaisseSolde(item.caisse.id).collectAsState(initial = 0L).value
                        CaisseDetailCard(
                            caisse = item.caisse,
                            soldeGlobal = solde ?: 0L,
                            onClick = {
                                onCaisseClick(item.caisse.id, item.activeCycleId)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "MEMBRES DE LA TONTINE (${uiState.displayMembers.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (tontine.isAnonymous) {
                            Text(
                                "MODE ANONYME",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.displayMembers.forEach { member ->
                        MemberItem(
                            member = member,
                            canEdit = uiState.role == Role.INITIATEUR || uiState.role == Role.PRESIDENT,
                            onEditClick = { selectedMemberForEdit = member }
                        )
                    }

                    Spacer(modifier = Modifier.height(100.dp)) // Espace pour ne pas cacher le dernier membre par le FAB
                }
            }
            }
        }
    }
}