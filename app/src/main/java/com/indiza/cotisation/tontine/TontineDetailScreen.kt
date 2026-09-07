package com.indiza.cotisation.tontine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.caisses.CaisseConfigScreen
import com.indiza.cotisation.caisses.CaisseType
import com.indiza.cotisation.tontinemembers.AddMemberDialog
import com.indiza.cotisation.tontinemembers.EditMemberDialog
import com.indiza.cotisation.tontinemembers.MemberItem
import com.indiza.cotisation.tontinemembers.MemberUiModel
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan
import com.indiza.cotisation.users.Permission
import com.indiza.cotisation.users.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TontineDetailScreen(
    tontineId: Long,
    viewModel: TontineDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCaisseClick: (Long, Long) -> Unit,
    onLoanManagementClick: (Long) -> Unit,
    onMemberClick: (Long, Long) -> Unit,
    onEditTontine: (Long) -> Unit
) {
    LaunchedEffect(tontineId) { viewModel.loadTontine(tontineId) }

    val state by viewModel.state.collectAsState()
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var selectedMemberForEdit by remember { mutableStateOf<MemberUiModel?>(null) }
    var selectedMemberForInfo by remember { mutableStateOf<MemberUiModel?>(null) }
    var showTypeSelection by remember { mutableStateOf(false) }
    var selectedTypeForConfig by remember { mutableStateOf<CaisseType?>(null) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

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
            takenUniqueRoles = (state as? TontineDetailUiState.Success)?.takenUniqueRoles ?: emptySet(),
            onDismiss = { selectedMemberForEdit = null },
            onConfirm = { n, p, r ->
                viewModel.updateMember(member.id, n, p, r)
                selectedMemberForEdit = null
            },
            onMapRequest = {
                selectedMemberForEdit = null
            }
        )
    }

    selectedMemberForInfo?.let { member ->
        onMemberClick(tontineId, member.id.toLong())
        selectedMemberForInfo = null
    }

    CosmicBackground {
        when (val uiState = state) {
            is TontineDetailUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            is TontineDetailUiState.Success -> {
                val tontine = uiState.tontineWithCaisses.tontine
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            if (uiState.canConsultLoans) {
                                ExtendedFloatingActionButton(
                                    onClick = { onLoanManagementClick(tontineId) },
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                    icon = { Icon(Icons.Default.Payments, null) },
                                    text = { Text("Gestion des Prêts") }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            ExtendedFloatingActionButton(
                                onClick = { showAddMemberDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                icon = { Icon(Icons.Default.PersonAdd, null) },
                                text = { Text("Ajouter Membre") }
                            )
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        if (showAddMemberDialog) {
                            AddMemberDialog(
                                takenUniqueRoles = (uiState as? TontineDetailUiState.Success)?.takenUniqueRoles ?: emptySet(),
                                onDismiss = { showAddMemberDialog = false },
                                onConfirm = { n, p, r ->
                                    viewModel.addMember(tontine.id, n, p, r)
                                    showAddMemberDialog = false
                                }
                            )
                        }
                        
                        // Header
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
                                    Icon(Icons.Default.ArrowBack, "Retour", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = tontine.libelle.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = uiState.role.displayName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { onEditTontine(tontineId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Paramètres",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- BLOC RÉSUMÉ FINANCIER ---
                        TontineFinancialSummaryBlock(uiState.financialSummary)

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        QuickActionsRow(uiState.role)
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        // Liste des Caisses
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CAISSES ACTIVES", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                            Box {
                                IconButton(onClick = { filterMenuExpanded = true }) {
                                    Icon(Icons.Default.FilterList, "Trier/Grouper", tint = MaterialTheme.colorScheme.primary)
                                }
                                DropdownMenu(expanded = filterMenuExpanded, onDismissRequest = { filterMenuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Nom (A-Z)") }, onClick = { viewModel.setFilterOption(CaisseFilterOption.ALPHABETICAL); filterMenuExpanded = false })
                                    DropdownMenuItem(text = { Text("Date (Ancien → Récent)") }, onClick = { viewModel.setFilterOption(CaisseFilterOption.CREATION_DATE_ASC); filterMenuExpanded = false })
                                    DropdownMenuItem(text = { Text("Grouper par Type") }, onClick = { viewModel.setFilterOption(CaisseFilterOption.GROUP_BY_TYPE); filterMenuExpanded = false })
                                }
                            }
                        }

                        if (uiState.role.permissions.contains(Permission.EDIT_TONTINE_RULES) || uiState.isInitiator) {
                            TextButton(onClick = { showTypeSelection = true }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Nouvelle Caisse", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        when (uiState.filterOption) {
                            CaisseFilterOption.GROUP_BY_TYPE -> {
                                uiState.caissesWithCycles.groupBy { it.caisse.type }.forEach { (type, items) ->
                                    Text(text = type.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                                    items.forEach { item -> CaisseItemRow(item, viewModel, onCaisseClick) }
                                }
                            }
                            else -> uiState.caissesWithCycles.forEach { item -> CaisseItemRow(item, viewModel, onCaisseClick) }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MEMBRES DE LA TONTINE (${uiState.displayMembers.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            if (tontine.isAnonymous) Text("MODE ANONYME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(end = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.displayMembers.forEach { member ->
                            MemberItem(
                                member = member,
                                canEdit = uiState.role == Role.INITIATEUR || uiState.role == Role.PRESIDENT,
                                onEditClick = { selectedMemberForEdit = member },
                                onClick = { selectedMemberForInfo = member }
                            )
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MemberInfoDialog(
    member: MemberUiModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Rôle: ${member.role.displayName}")
                }
                Text("Téléphone: ${member.phoneNumber}")
                if (member.matricule.isNotBlank()) {
                    Text("Matricule: ${member.matricule}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
fun TontineFinancialSummaryBlock(summary: TontineFinancialSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("RÉSUMÉ FINANCIER GLOBAL", style = MaterialTheme.typography.titleSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(Modifier.weight(1f), "Balance Globale", "${summary.globalBalance} F", NeonCyan)
                SummaryItem(Modifier.weight(1f), "Chiffre d'Affaires", "${summary.turnoverIn + summary.turnoverOut} F", Color.White)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(Modifier.weight(1f), "Prêts Actifs", "${summary.totalActiveLoans} F", Color(0xFFFF7A59))
                SummaryItem(Modifier.weight(1f), "Gains Prévus (Intérêts)", "${summary.forecastGains} F", Color(0xFF50FA7B))
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(Modifier.weight(1f), "Montant Prêtable", "${summary.lendableAmount} F", Color(0xFF8BE9FD))
                SummaryItem(Modifier.weight(1f), "Non Prêtable", "${summary.nonLendableAmount} F", Color.Gray)
            }
            
            if (summary.lendableAmount > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = summary.totalActiveLoans.toFloat() / (summary.lendableAmount + summary.totalActiveLoans).coerceAtLeast(1L),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = Color(0xFFFF7A59),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text(
                    "Utilisation de la capacité de prêt : ${(summary.totalActiveLoans * 100 / (summary.lendableAmount + summary.totalActiveLoans).coerceAtLeast(1L))}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryItem(modifier: Modifier, label: String, value: String, color: Color) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CaisseItemRow(
    item: com.indiza.cotisation.caisses.CaisseWithActiveCycle,
    viewModel: TontineDetailViewModel,
    onCaisseClick: (Long, Long) -> Unit
) {
    val solde = viewModel.getCaisseSolde(item.caisse.id).collectAsState(initial = 0L).value
    CaisseDetailCard(
        caisse = item.caisse,
        soldeGlobal = solde ?: 0L,
        onClick = {
            onCaisseClick(item.caisse.id, item.activeCycleId)
        }
    )
}
