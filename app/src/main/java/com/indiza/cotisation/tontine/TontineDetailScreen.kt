package com.indiza.cotisation.tontine

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.bank.TransactionEntity
import com.indiza.cotisation.bank.TransactionType
import com.indiza.cotisation.caisses.CaisseConfigScreen
import com.indiza.cotisation.caisses.CaisseType
import com.indiza.cotisation.caisses.CaisseWithActiveCycle
import com.indiza.cotisation.tontinemembers.AddMemberDialog
import com.indiza.cotisation.tontinemembers.EditMemberDialog
import com.indiza.cotisation.tontinemembers.MemberItem
import com.indiza.cotisation.tontinemembers.MemberUiModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.AfroOrange
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan
import com.indiza.cotisation.users.Permission
import com.indiza.cotisation.users.Role

enum class TontineStatType { FINANCIAL, GROWTH, INCOME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TontineDetailScreen(
    tontineId: Long,
    viewModel: TontineDetailViewModel = hiltViewModel(),
    navController: androidx.navigation.NavController,
    onBack: () -> Unit,
    onCaisseClick: (Long, Long) -> Unit,
    onLoanManagementClick: (Long) -> Unit,
    onMemberClick: (Long, Long) -> Unit,
    onEditTontine: (Long) -> Unit
) {
    LaunchedEffect(tontineId) { viewModel.loadTontine(tontineId) }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var selectedMemberForEdit by remember { mutableStateOf<MemberUiModel?>(null) }
    var selectedMemberForInfo by remember { mutableStateOf<MemberUiModel?>(null) }
    var showTypeSelection by remember { mutableStateOf(false) }
    var selectedTypeForConfig by remember { mutableStateOf<CaisseType?>(null) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    if (showReportSheet && state is TontineDetailUiState.Success) {
        val successState = state as TontineDetailUiState.Success
        val cycleIds = successState.caissesWithCycles.map { it.activeCycleId }
        val transactions by viewModel.getTransactionsForCycles(cycleIds).collectAsState(initial = emptyList())
        
        MemberContributionReportSheet(
            tontineName = successState.tontineWithCaisses.tontine.libelle,
            members = successState.displayMembers,
            caissesWithCycles = successState.caissesWithCycles,
            transactions = transactions,
            onDismiss = { showReportSheet = false }
        )
    }

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
            is TontineDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) 
            }
            is TontineDetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = Color.Red)
            }
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

                                if (tontine.isOnline) {
                                    Surface(
                                        color = Color(0xFFE0F7FA),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFF00BCD4))
                                    ) {
                                        Text(
                                            "LIVE",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF0097A7),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                } else {
                                    // Option Passer en ligne
                                    if (uiState.financialSummary.globalBalance == 0L && uiState.displayMembers.size <= 1) {
                                        TextButton(
                                            onClick = { viewModel.goOnline(tontineId) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Icon(Icons.Default.CloudQueue, null, modifier = Modifier.size(12.dp), tint = NeonCyan)
                                            Spacer(Modifier.width(4.dp))
                                            Text("PASSER EN LIGNE", fontSize = 8.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

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

                                IconButton(
                                    onClick = { showReportSheet = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = "Fiche de Cotisation",
                                        tint = AfroGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { 
                                        if (uiState is TontineDetailUiState.Success) {
                                            com.indiza.cotisation.tools.PdfReportGenerator.generateAndShare(context, uiState)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Partager PDF",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { navController.navigate("meeting_chat/$tontineId") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Chat de réunion",
                                        tint = AfroOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- STATISTIQUES COMPACTES ---
                        var expandedStat by remember { mutableStateOf<TontineStatType?>(null) }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.MonetizationOn,
                                label = "Finance",
                                isSelected = expandedStat == TontineStatType.FINANCIAL,
                                onClick = { expandedStat = if (expandedStat == TontineStatType.FINANCIAL) null else TontineStatType.FINANCIAL }
                            )
                            StatTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.AutoMirrored.Filled.ShowChart,
                                label = "Évolution",
                                isSelected = expandedStat == TontineStatType.GROWTH,
                                onClick = { expandedStat = if (expandedStat == TontineStatType.GROWTH) null else TontineStatType.GROWTH }
                            )
                            StatTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.PieChart,
                                label = "Revenus",
                                isSelected = expandedStat == TontineStatType.INCOME,
                                onClick = { expandedStat = if (expandedStat == TontineStatType.INCOME) null else TontineStatType.INCOME }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedVisibility(
                            visible = expandedStat == TontineStatType.FINANCIAL,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            TontineFinancialSummaryBlock(uiState.financialSummary)
                        }

                        AnimatedVisibility(
                            visible = expandedStat == TontineStatType.GROWTH,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("CROISSANCE TRÉSORERIE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(12.dp))
                                    com.indiza.cotisation.tools.GrowthLineChart(
                                        data = uiState.growthStats,
                                        modifier = Modifier.fillMaxWidth().height(120.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = expandedStat == TontineStatType.INCOME,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("RÉPARTITION DES REVENUS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        uiState.incomeStats.forEachIndexed { index, stats ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).background(
                                                    listOf(NeonCyan, AfroOrange, Color(0xFF50FA7B), Color(0xFFA855F7), Color(0xFFFF5555))[index % 5],
                                                    CircleShape
                                                ))
                                                Spacer(Modifier.width(8.dp))
                                                Text("${stats.type}: ${stats.total} F", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                            }
                                        }
                                    }
                                    com.indiza.cotisation.tools.IncomeDistributionChart(
                                        data = uiState.incomeStats,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- ADMINISTRATION (INVITATIONS & DEMANDES) ---
                        if (uiState.role != Role.MEMBRE || uiState.isInitiator) {
                            AdminActionsRow(
                                tontineId = tontineId,
                                viewModel = viewModel,
                                onNavigateToRequests = { 
                                    navController.navigate("join_requests/$tontineId")
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
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
                                    items.forEach { item -> 
                                        CaisseItemRow(item, viewModel, onCaisseClick) { id ->
                                            viewModel.deleteCaisse(id) { result ->
                                                result.onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> uiState.caissesWithCycles.forEach { item -> 
                                CaisseItemRow(item, viewModel, onCaisseClick) { id ->
                                    viewModel.deleteCaisse(id) { result ->
                                        result.onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            }
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
fun StatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color.White.copy(alpha = 0.1f)),
        modifier = modifier.height(64.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NeonCyan else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                color = if (isSelected) Color.White else Color.Gray
            )
        }
    }
}

@Composable
fun AdminActionsRow(
    tontineId: Long,
    viewModel: TontineDetailViewModel,
    onNavigateToRequests: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bouton Inviter
        Button(
            onClick = {
                viewModel.generateInviteLink(tontineId) { url ->
                    if (url != null) {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Rejoins ma tontine sur Cotisation : $url")
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Inviter via")
                        context.startActivity(shareIntent)
                    } else {
                        Toast.makeText(context, "Erreur lors de la génération du lien", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.weight(1f).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("INVITER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Bouton Diffuser (Notification)
        Button(
            onClick = {
                viewModel.broadcastReport(tontineId) { success ->
                    val msg = if (success) "Rapport diffusé aux membres !" else "Erreur de diffusion"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1.3f).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("DIFFUSER", fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }

        // Bouton Demandes
        Button(
            onClick = onNavigateToRequests,
            modifier = Modifier.weight(1f).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Icon(Icons.Default.NotificationImportant, null, modifier = Modifier.size(18.dp), tint = AfroGold)
            Spacer(Modifier.width(8.dp))
            Text("DEMANDES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    onCaisseClick: (Long, Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val solde = viewModel.getCaisseSolde(item.caisse.id).collectAsState(initial = 0L).value
    CaisseDetailCard(
        caisse = item.caisse,
        soldeGlobal = solde ?: 0L,
        onClick = {
            onCaisseClick(item.caisse.id, item.activeCycleId)
        },
        onDelete = { onDelete(item.caisse.id) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberContributionReportSheet(
    tontineName: String,
    members: List<MemberUiModel>,
    caissesWithCycles: List<CaisseWithActiveCycle>,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    
    // 1. Définition des colonnes
    val reportColumns = remember(caissesWithCycles) {
        val cols = mutableListOf<ReportColumn>()
        caissesWithCycles.forEach { item ->
            if (item.caisse.type == CaisseType.PRET) {
                cols.add(ReportColumn("EMPRUNT", item.caisse.id, ReportColumnType.LOAN_TAKEN))
                cols.add(ReportColumn("REMB CAP", item.caisse.id, ReportColumnType.REPAY_CAPITAL))
                cols.add(ReportColumn("REMB INT", item.caisse.id, ReportColumnType.REPAY_INTEREST))
            } else {
                val title = when {
                    item.caisse.libelle.contains("COTIS", true) -> "COTISAT"
                    item.caisse.libelle.contains("SECOURS", true) -> "SECOURS"
                    item.caisse.libelle.contains("RATION", true) -> "RATION"
                    item.caisse.libelle.contains("QUIN", true) -> "QUININE"
                    item.caisse.libelle.contains("INS", true) -> "INS/REIN"
                    else -> item.caisse.libelle.take(8).uppercase()
                }
                cols.add(ReportColumn(title, item.caisse.id, ReportColumnType.NORMAL))
            }
        }
        cols
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("FICHE DE COTISATION", fontWeight = FontWeight.Black, fontSize = 14.sp) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White, titleContentColor = Color.Black)
                )
            },
            containerColor = Color.White
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.White)
            ) {
                // Table
                Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState)) {
                    Column {
                        // Header de la table
                        Row(modifier = Modifier.background(Color.LightGray.copy(alpha = 0.3f))) {
                            TableCell("N°", 40.dp, true)
                            TableCell("NOMS ET PRENOMS", 160.dp, true)
                            reportColumns.forEach { col ->
                                TableCell(col.title, 70.dp, true)
                            }
                            TableCell("AUTRES", 60.dp, true)
                        }

                        // Lignes des membres
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(members.size) { index ->
                                val member = members[index]
                                Row(modifier = Modifier.border(0.2.dp, Color.Black)) {
                                    TableCell("${index + 1}", 40.dp)
                                    TableCell(member.displayName.uppercase(), 160.dp, alignment = TextAlign.Start)
                                    
                                    reportColumns.forEach { col ->
                                        val value = transactions.filter { 
                                            it.memberId == member.id.toLong() && it.caisseId == col.caisseId 
                                        }.let { list ->
                                            when (col.type) {
                                                ReportColumnType.NORMAL -> list.filter { it.type == TransactionType.COTISATION && it.a_cotise }.sumOf { it.montant }
                                                ReportColumnType.LOAN_TAKEN -> list.filter { it.type == TransactionType.PRET }.sumOf { -it.montant }
                                                ReportColumnType.REPAY_CAPITAL -> list.filter { it.type == TransactionType.REMBOURSEMENT }.sumOf { it.montant }
                                                ReportColumnType.REPAY_INTEREST -> list.filter { it.type == TransactionType.INTERET }.sumOf { it.montant }
                                            }
                                        }
                                        TableCell(if (value > 0) value.toString() else "", 70.dp)
                                    }
                                    TableCell("", 60.dp)
                                }
                            }
                        }
                    }
                }

                // Résumé du bas dynamique (Somme des caisses en cours)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(1.dp, Color.Black)
                ) {
                    var totalGeneral = 0L
                    
                    caissesWithCycles.forEach { item ->
                        val totalCaisse = transactions.filter { it.caisseId == item.caisse.id }
                            .sumOf { tx ->
                                when (tx.type) {
                                    TransactionType.COTISATION -> if (tx.a_cotise) tx.montant else 0L
                                    TransactionType.REMBOURSEMENT, TransactionType.INTERET -> tx.montant
                                    else -> 0L 
                                }
                            }
                        SummaryRow("TOTAL ${item.caisse.libelle.uppercase()}", totalCaisse)
                        totalGeneral += totalCaisse
                    }

                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                    SummaryRow("TOTAL MONTANT PERCU", totalGeneral)
                }
            }
        }
    }
}

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, isHeader: Boolean = false, alignment: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .border(0.5.dp, Color.Black)
            .padding(4.dp),
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isHeader) 10.sp else 9.sp,
            color = Color.Black
        ),
        textAlign = alignment,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun SummaryRow(label: String, value: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.Black),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1.5f).padding(4.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
        Text(value.toString(), modifier = Modifier.weight(1f).border(0.5.dp, Color.Black).padding(4.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
    }
}

data class ReportColumn(val title: String, val caisseId: Long?, val type: ReportColumnType)
enum class ReportColumnType { NORMAL, LOAN_TAKEN, REPAY_CAPITAL, REPAY_INTEREST }
