package com.indiza.cotisation.caisses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Casino
import com.indiza.cotisation.users.Role
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.tools.NeonCyan
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.indiza.cotisation.Screen
import com.indiza.cotisation.bank.TransactionViewModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.AfroSunsetGradient
import com.indiza.cotisation.tools.DeepWhite
import com.indiza.cotisation.tools.GlassWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaisseDetailScreen(
    caisseId: Long,
    cycleId: Long,
    navController: NavController,
    viewModel: CaisseDetailViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    onBack: () -> Unit
) {

    var showEditCaisse by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    val accentColor = when(state.caisse?.type) {
        CaisseType.PRET -> Color(0xFF2196F3)        // Bleu
        CaisseType.SECOURS -> Color(0xFFE91E63)      // Rose/Rouge
        CaisseType.DEVELOPPEMENT -> Color(0xFFFF9800) // Orange
        CaisseType.COTISATION_SIMPLE -> NeonCyan    // Cyan original
        CaisseType.FONCTIONNEMENT -> Color(0xFF9C27B0) // Violet
        else -> NeonCyan
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var isSavingParticipants by remember { mutableStateOf(false) }
    val participantSnackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showConfirmShuffle by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    var cycleMenuExpanded by remember { mutableStateOf(false) }
    var showToursDialog by remember { mutableStateOf(false) }
    var showBreakdownDialog by remember { mutableStateOf(false) }

    var showManualMvtDialog by remember { mutableStateOf(false) }
    var manualMvtType by remember { mutableStateOf<com.indiza.cotisation.bank.TransactionType>(com.indiza.cotisation.bank.TransactionType.COTISATION) }
    var manualMvtMontant by remember { mutableStateOf("") }
    var manualMvtMotif by remember { mutableStateOf("") }
    var manualMvtMemberId by remember { mutableStateOf<Long?>(null) }

    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateCaisseName by remember { mutableStateOf("") }
    var showMustCloseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cycleId) {
        viewModel.selectCycleById(cycleId)
    }

    val cycle by viewModel.getCycle(cycleId).collectAsState(initial = null)

    val nouveauCycleId by transactionViewModel.nouveauCycleId.collectAsState()


    if (state.accessDenied) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onBack) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A1A),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Text("Accès Refusé", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        "Vous n'avez pas les droits nécessaires pour accéder à cette caisse. Vous devez être membre ou initiateur de la tontine.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("RETOUR")
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(participantSnackbarState) },
        floatingActionButton = {
            if (!state.isReadOnly && state.role != Role.MEMBRE) {
                val rolesAutorises = listOf(
                    com.indiza.cotisation.users.Role.PRESIDENT,
                    com.indiza.cotisation.users.Role.TRESORIER,
                    com.indiza.cotisation.users.Role.TRESORIER_ADJOINT_TA2
                )
                val peutGererManuellement = rolesAutorises.contains(state.role)

                Column(horizontalAlignment = Alignment.End) {
                    if (showActionMenu) {
                        CaisseActionButtons(
                            caisseType = state.caisse?.type,
                            possibilitePret = state.caisse?.possibilitePret ?: false,
                            peutGererManuellement = peutGererManuellement,
                            onActionSelected = { action ->
                                showActionMenu = false
                                when (action) {
                                    com.indiza.cotisation.CaisseAction.VOIR_TOURS -> showToursDialog = true
                                    com.indiza.cotisation.CaisseAction.TRANSFERT_ENTREE -> {
                                        manualMvtType = com.indiza.cotisation.bank.TransactionType.TRANSFERT_ENTREE
                                        showManualMvtDialog = true
                                    }
                                    com.indiza.cotisation.CaisseAction.SORTIE -> {
                                        manualMvtType = com.indiza.cotisation.bank.TransactionType.SORTIE
                                        showManualMvtDialog = true
                                    }
                                    com.indiza.cotisation.CaisseAction.RELANCE -> {
                                        // TODO: Implémenter le dialogue de sélection des retardataires
                                    }
                                    else -> {
                                        val currentCycleId = state.currentCycle?.id ?: cycleId
                                        navController.navigate("selection_membre/$caisseId/${action.name}/$currentCycleId")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    ExtendedFloatingActionButton(
                        text = { Text(if (showActionMenu) "Fermer" else "Actions") },
                        icon = {
                            Icon(
                                if (showActionMenu) Icons.Default.Close else Icons.Default.Add,
                                null
                            )
                        },
                        onClick = { showActionMenu = !showActionMenu },
                        containerColor = accentColor,
                        contentColor = if (accentColor == NeonCyan) Color.Black else Color.White
                    )
                }
            }
        }
    ) { padding ->

        if (showEditCaisse) {
            Dialog(
                onDismissRequest = { showEditCaisse = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                CaisseConfigScreen(
                    typeInitial = state.caisse?.type ?: CaisseType.COTISATION_SIMPLE,
                    existingCaisse = state.caisse,
                    isAmountLocked = (state.currentCycle?.numeroCycle ?: 1) > 1 || state.dejaCotise.isNotEmpty() || state.toursDetailCycle.isNotEmpty(),
                    onSave = { draft ->
                        viewModel.updateCaisse(draft)
                        showEditCaisse = false
                    },
                    onCancel = { showEditCaisse = false }
                )
            }
        }

        if (nouveauCycleId != null) {
            AlertDialog(
                onDismissRequest = {
                    transactionViewModel.clearNavigationRequest()
                },
                title = {
                    Text("Nouveau cycle créé")
                },
                text = {
                    Text("Le cycle suivant a été créé avec succès. Souhaitez-vous y accéder maintenant ?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            navController.navigate(
                                Screen.CaisseDetail.createRoute(caisseId, nouveauCycleId!!)
                            )
                            transactionViewModel.clearNavigationRequest()
                        }
                    ) {
                        Text("Oui")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            transactionViewModel.clearNavigationRequest()
                        }
                    ) {
                        Text("Plus tard")
                    }
                }
            )
        }

        if (showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = { showDuplicateDialog = false },
                title = { Text("Dupliquer la Caisse", color = Color.White) },
                containerColor = Color(0xFF1A1E2A),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Cette action va créer une nouvelle caisse avec les mêmes paramètres et participants.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        OutlinedTextField(
                            value = duplicateCaisseName,
                            onValueChange = { duplicateCaisseName = it },
                            label = { Text("Nom de la nouvelle caisse") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.duplicateCaisse(duplicateCaisseName) { newId ->
                                showDuplicateDialog = false
                                // Naviguer vers la nouvelle caisse
                                navController.navigate(Screen.CaisseDetail.createRoute(newId, 0L))
                            }
                        },
                        enabled = duplicateCaisseName.isNotBlank()
                    ) {
                        Text("DUPLIQUER", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDuplicateDialog = false }) {
                        Text("ANNULER", color = Color.Gray)
                    }
                }
            )
        }

        if (showMustCloseDialog) {
            AlertDialog(
                onDismissRequest = { showMustCloseDialog = false },
                title = { Text("Action requise", color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF1A1E2A),
                text = {
                    Text(
                        "Un cycle est actuellement ouvert. Vous devez impérativement le clôturer avant de pouvoir changer de cycle dans le sélecteur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showMustCloseDialog = false }) {
                        Text("COMPRIS", color = AfroGold, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(
                color = Color(0xFF111827).copy(alpha = 0.85f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = GlassWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = state.caisse?.libelle?.uppercase() ?: "DÉTAILS",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val currentCycleId = state.currentCycle?.id ?: cycleId
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.ReportsMenu.createRoute(caisseId, currentCycleId))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Rapports",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                navController.navigate(Screen.TransactionJournal.createRoute(caisseId, currentCycleId))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Journal",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        val hideGlobalInfo = state.role == Role.MEMBRE
                        if (!hideGlobalInfo && state.role != Role.MEMBRE) {
                            IconButton(
                                onClick = { showEditCaisse = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Settings, "Paramètres", tint = accentColor)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.caisse?.type != CaisseType.PRET && state.role != Role.MEMBRE) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { cycleMenuExpanded = true },
                    color = Color(0xFF1A1E2A),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "CYCLE ACTUEL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AfroGold
                                )
                                Text(
                                    text = "Cycle n°${state.currentCycle?.numeroCycle ?: 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.currentCycle?.estCloture == true) {
                                    Text(
                                        "ARCHIVÉ",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier
                                            .background(Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color.White)
                            }
                        }

                        DropdownMenu(
                            expanded = cycleMenuExpanded,
                            onDismissRequest = { cycleMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF1A1A1A)).border(1.dp, GlassWhite, RoundedCornerShape(8.dp))
                        ) {
                            state.allCycles.forEach { cycle ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                        "Cycle n°${cycle.numeroCycle} ${if (cycle.estCloture) "(Clôturé)" else "(Actif)"}",
                                        color = if (cycle.id == state.currentCycle?.id) accentColor else Color.White
                                    )
                                    },
                                    onClick = {
                                        viewModel.selectCycle(cycle)
                                        cycleMenuExpanded = false
                                    }
                                )
                            }
                        }

                        if (!state.isReadOnly || state.peutCloturerTour || state.peutDupliquer || (state.currentCycle?.estCloture == true && (state.role == Role.INITIATEUR || state.role == Role.PRESIDENT))) {
                            val currentCycleId = state.currentCycle?.id ?: cycleId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cycle?.let { cycleEntity ->
                                    if (state.peutCloturerCycle) {
                                        val hasHigher = state.allCycles.any { it.numeroCycle > cycleEntity.numeroCycle }
                                        val buttonLabel = if (hasHigher) "Refermer" else "Clôturer Cycle"
                                        AdminButton(
                                            text = buttonLabel,
                                            color = Color.Red,
                                            icon = Icons.Default.Close,
                                            onClick = {
                                                transactionViewModel.cloturerEtPreparerSuivant(
                                                    caisseId,
                                                    cycleEntity
                                                )
                                            }
                                        )
                                    }

                                    if (cycleEntity.estCloture && (state.role == Role.INITIATEUR || state.role == Role.PRESIDENT)) {
                                        AdminButton(
                                            text = "Réouvrir",
                                            color = AfroGold,
                                            icon = Icons.Default.Refresh,
                                            onClick = {
                                                transactionViewModel.reouvrirCycle(cycleEntity)
                                            }
                                        )
                                    }

                                    if (state.peutCloturerDefinitivement) {
                                        AdminButton(
                                            text = "Clôturer",
                                            color = Color.Red,
                                            icon = Icons.Default.Close,
                                            onClick = {
                                                transactionViewModel.cloturerCaisseDefinitivement(cycleEntity) {
                                                    navController.popBackStack()
                                                }
                                            }
                                        )
                                    }
                                }

                                if (state.peutDupliquer) {
                                    val isRedistrib = state.caisse?.redistribution == true
                                    val buttonText = if (isRedistrib) "Redémarrer" else "Dupliquer"
                                    AdminButton(
                                        text = buttonText,
                                        color = if (isRedistrib) AfroGold else accentColor,
                                        icon = if (isRedistrib) Icons.Default.Refresh else Icons.Default.AddCard,
                                        onClick = {
                                            duplicateCaisseName = if (isRedistrib) state.caisse?.libelle ?: "" else "${state.caisse?.libelle} (Nouveau)"
                                            showDuplicateDialog = true
                                        }
                                    )
                                }

                                if (state.peutAjouterTour && !(state.tontine?.hideInfoForSimpleMembers == true && state.role == Role.MEMBRE)) {
                                    AdminButton(
                                        text = "Ajouter Tour",
                                        color = AfroGold,
                                        icon = Icons.Default.Add,
                                        onClick = {
                                            navController.navigate("creation_tour/${caisseId}/${currentCycleId}")
                                        }
                                    )
                                }

                                if (state.participantsPlanifies.isNotEmpty()) {
                                    AdminButton(
                                        text = "Annuler Tour",
                                        color = Color.Red,
                                        icon = Icons.Default.RemoveCircleOutline,
                                        onClick = {
                                            viewModel.annulerTousLesToursPlanifies(currentCycleId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                // 1. En-tête principal
                item {
                    val hideGlobalInfo = state.role == Role.MEMBRE
                    if (hideGlobalInfo) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = state.caisse?.libelle?.uppercase() ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box {
                                        Surface(
                                            onClick = { cycleMenuExpanded = !cycleMenuExpanded },
                                            color = NeonCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.5.dp, NeonCyan)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "CYCLE ${state.currentCycle?.numeroCycle ?: 1}${if (state.currentCycle?.estCloture == true) " (Clôturé)" else ""}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeonCyan,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Default.ExpandMore, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = cycleMenuExpanded,
                                            onDismissRequest = { cycleMenuExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1A1A1A)).border(1.dp, GlassWhite, RoundedCornerShape(8.dp))
                                        ) {
                                            state.allCycles.forEach { cycle ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "Cycle n°${cycle.numeroCycle} ${if (cycle.estCloture) "(Clôturé)" else "(Actif)"}",
                                                            color = if (cycle.id == state.currentCycle?.id) NeonCyan else Color.White
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.selectCycle(cycle)
                                                        cycleMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Surface(
                                    color = (if (state.currentMemberHasContributed) Color(0xFF4CAF50) else AfroGold).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (state.currentMemberHasContributed) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = if (state.currentMemberHasContributed) Color(0xFF4CAF50) else AfroGold
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = if (state.currentMemberHasContributed) "Statut : À jour (Cotisé pour ce cycle)" else "Statut : En attente de cotisation",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DetailMetricCard(
                                        modifier = Modifier.weight(1f),
                                        label = "COTISATION",
                                        value = state.caisse?.montantFixe?.let { "$it F" } ?: "Libre",
                                        accentColor = NeonCyan,
                                        helper = "Tarif Cible"
                                    )
                                    DetailMetricCard(
                                        modifier = Modifier.weight(1f),
                                        label = "AMENDES",
                                        value = "${state.caisse?.penaliteRetardFixe ?: 0L} F",
                                        accentColor = AfroGold,
                                        helper = "Retard"
                                    )
                                    DetailMetricCard(
                                        modifier = Modifier.weight(1f),
                                        label = "PÉNALITÉS",
                                        value = "${state.caisse?.penaliteEchecFixe ?: 0L} F",
                                        accentColor = Color(0xFFFF7A59),
                                        helper = "Échec"
                                    )
                                    DetailMetricCard(
                                        modifier = Modifier.weight(1f),
                                        label = "DATE LIMITE",
                                        value = state.caisse?.dateLimiteJour?.let { "Jour $it" } ?: "—",
                                        accentColor = Color(0xFF8BE9FD),
                                        helper = "Paiement"
                                    )
                                }
                            }
                        }
                    } else {
                        CaisseHeader(
                            caisse = state.caisse,
                            currentCycle = state.currentCycle,
                            soldeDuCycle = state.soldeGlobal,
                            soldePrecedent = state.soldePrecedent,
                            totalPrets = state.totalPrets,
                            toursCompletes = state.toursCompletes,
                            nombreCotisants = state.dejaCotise.size,
                            nombreBeneficiaires = state.dejaBeneficiaires.size,
                            prochainBeneficiaire = state.beneficiaireActuel?.memberWithUser?.user?.nom,
                            totalParticipants = state.participants.size,
                            montantCible = ((state.caisse?.montantFixe ?: 0L) * state.participants.sumOf { it.nombreParts }.coerceAtLeast(1)) * state.toursDuCycle.size.coerceAtLeast(1),
                            dateLimitePaiement = state.caisse?.dateLimiteJour,
                            totalAmendes = state.totalAmendes,
                            totalPenalites = state.totalPenalites,
                            montantDistribue = state.montantDistribue,
                            beneficiaires = state.beneficiairesDuCycle.map { it.member.member.nomSaisi }.distinct(),
                            montantRecolte = state.montantRecolte,
                            onReportClick = {
                                val currentCycle = state.currentCycle
                                val actualCycleId = currentCycle?.id ?: cycleId
                                val cycleNum = currentCycle?.numeroCycle ?: 1
                                navController.navigate("cycle_report/$caisseId/$actualCycleId/$cycleNum")
                            },
                            onToggleLock = {
                                viewModel.toggleOrdreFige()
                            },
                            onBalanceClick = {
                                showBreakdownDialog = true
                            }
                        )
                    }
                }

                // 2. Header Fixe des Participants
                stickyHeader {
                    Surface(
                        color = Color(0xFF111827).copy(alpha = 0.95f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PARTICIPANTS (${state.participants.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (state.caisse?.redistribution == true && !state.isReadOnly) {
                                    IconButton(
                                        onClick = { showConfirmShuffle = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(AfroGold, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Casino,
                                            contentDescription = "Tirage au sort",
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (state.peutAjouterMembres && state.role != Role.MEMBRE) {
                                    Surface(
                                        onClick = {
                                            navController.navigate(Screen.ManageParticipants.createRoute(caisseId))
                                        },
                                        shape = CircleShape,
                                        color = AfroGold,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.GroupAdd,
                                                contentDescription = "Gérer les participants",
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Liste des Participants
                itemsIndexed(
                    items = state.participants,
                    key = { _, participant -> participant.memberWithUser.member.id }
                ) { index, participant ->
                    val memberId = participant.memberWithUser.member.id
                    val estPlanifie = state.participantsPlanifies.any { it.memberWithUser.member.id == memberId }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(), // Animation fluide du changement d'ordre
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1F2937),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                val aCotiseCeCycle = state.dejaCotise.contains(memberId)
                                val partsRecuesCeMembre = state.partsEncaisseesJusquiciMap[memberId] ?: 0

                                ParticipantOrderRow(
                                    participant = participant,
                                    ordre = index + 1,
                                    isRedistribution = state.caisse?.redistribution ?: false,
                                    aCotiseCeCycle = aCotiseCeCycle,
                                    partsRecuesTotal = partsRecuesCeMembre,
                                    canReorder = state.role != Role.MEMBRE,
                                    onMoveUp = { if (state.caisse?.ordreFige != true) viewModel.moveParticipant(index, index - 1) },
                                    onMoveDown = { if (state.caisse?.ordreFige != true) viewModel.moveParticipant(index, index + 1) }
                                )
                            }

                            if (estPlanifie) {
                                val currentCycleId = state.currentCycle?.id ?: cycleId
                                IconButton(
                                    onClick = {
                                        transactionViewModel.annulerPositionnementCagnotte(caisseId, memberId, currentCycleId)
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.1f))
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonRemove,
                                        contentDescription = "Annuler tour/attribution",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showConfirmShuffle) {
            AlertDialog(
                onDismissRequest = { showConfirmShuffle = false },
                title = { Text("Lancer le tirage ?") },
                text = { Text("Cela va réinitialiser l'ordre de passage.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.shuffleParticipants()
                        showConfirmShuffle = false
                    }) { Text("CONFIRMER", color = NeonCyan) }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmShuffle = false }) { Text("ANNULER") }
                }
            )
        }

        if (showAddDialog) {
            AddParticipantsDialog(
                allTontineMembers = state.allTontineMembers,
                currentParticipants = state.participants,
                canAddMembers = state.peutAjouterMembres && !isSavingParticipants,
                membersThatCannotBeRemoved = if (isSavingParticipants) {
                    state.participants.mapTo(mutableSetOf()) { it.member.id }
                } else {
                    state.partsEncaisseesMap.filterValues { it > 0 }.keys
                },
                isSaving = isSavingParticipants,
                onSave = { selectedParts ->
                    if (!isSavingParticipants) {
                        coroutineScope.launch {
                            isSavingParticipants = true
                            try {
                                viewModel.saveCaisseParticipantSelection(selectedParts)
                                showAddDialog = false
                            } catch (e: Exception) {
                                participantSnackbarState.showSnackbar(
                                    e.message ?: "Impossible d'enregistrer les participants."
                                )
                            } finally {
                                isSavingParticipants = false
                            }
                        }
                    }
                },
            )
        }

        if (showToursDialog) {
            ScheduledToursDialog(
                tours = state.toursDetailCycle,
                onDismiss = { showToursDialog = false }
            )
        }

        if (showBreakdownDialog) {
            CaisseBreakdownDialog(
                state = state,
                onExport = { type: com.indiza.cotisation.bank.TransactionType, amount: Long, motif: String, targetId: Long? -> 
                    viewModel.exporterFond(type, amount, motif, targetId)
                    showBreakdownDialog = false
                },
                onDismiss = { showBreakdownDialog = false }
            )
        }

        if (showManualMvtDialog) {
            ManualMovementDialog(
                type = manualMvtType,
                montant = manualMvtMontant,
                onMontantChange = { manualMvtMontant = it },
                motif = manualMvtMotif,
                onMotifChange = { manualMvtMotif = it },
                isRedistribution = state.caisse?.redistribution ?: false,
                onDismiss = { 
                    showManualMvtDialog = false
                    manualMvtMontant = ""
                    manualMvtMotif = ""
                },
                onConfirm = {
                    val amt = manualMvtMontant.toLongOrNull() ?: 0L
                    val currentCycleId = state.currentCycle?.id ?: cycleId
                    if (amt > 0) {
                        transactionViewModel.enregistrerMouvementManuel(
                            caisseId = caisseId,
                            cycleId = currentCycleId,
                            montant = amt,
                            type = manualMvtType,
                            motif = manualMvtMotif,
                            memberId = null
                        ) {
                            showManualMvtDialog = false
                            manualMvtMontant = ""
                            manualMvtMotif = ""
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CaisseBreakdownDialog(
    state: CaisseDetailUiState,
    onExport: (com.indiza.cotisation.bank.TransactionType, Long, String, targetId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var showTargetSelectorForAmendes by remember { mutableStateOf(false) }
    var showTargetSelectorForFrais by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Détail du Solde", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF1A1E2A),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BreakdownItem("Cotisations", state.breakdown.find { it.type == com.indiza.cotisation.bank.TransactionType.COTISATION }?.total ?: 0L)
                BreakdownItem("Cagnottes versées", state.breakdown.find { it.type == com.indiza.cotisation.bank.TransactionType.TOUR_ENCAISSER }?.total ?: 0L, isNegative = true)
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                // Amendes
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        BreakdownItem("Amendes (Disponibles)", state.soldeAmendes, modifier = Modifier.weight(1f))
                        if (state.soldeAmendes > 0 && (state.role == com.indiza.cotisation.users.Role.PRESIDENT || state.role == com.indiza.cotisation.users.Role.TRESORIER)) {
                            TextButton(onClick = { showTargetSelectorForAmendes = true }) {
                                Text("SORTIR", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (showTargetSelectorForAmendes) {
                        TargetCaisseSelector(
                            caisses = state.allTontineCaisses.filter { it.id != state.caisse?.id },
                            onSelected = { targetId ->
                                onExport(com.indiza.cotisation.bank.TransactionType.PENALITE_RETARD, state.soldeAmendes, "Export des amendes", targetId)
                                showTargetSelectorForAmendes = false
                            },
                            onCancel = { showTargetSelectorForAmendes = false }
                        )
                    }
                }

                // Frais
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        BreakdownItem("Frais de caisse (Disponibles)", state.soldeFraisCaisse, modifier = Modifier.weight(1f))
                        if (state.soldeFraisCaisse > 0 && (state.role == com.indiza.cotisation.users.Role.PRESIDENT || state.role == com.indiza.cotisation.users.Role.TRESORIER)) {
                            TextButton(onClick = { showTargetSelectorForFrais = true }) {
                                Text("SORTIR", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (showTargetSelectorForFrais) {
                        TargetCaisseSelector(
                            caisses = state.allTontineCaisses.filter { it.id != state.caisse?.id },
                            onSelected = { targetId ->
                                onExport(com.indiza.cotisation.bank.TransactionType.FRAIS_CAISSE, state.soldeFraisCaisse, "Export des frais de caisse", targetId)
                                showTargetSelectorForFrais = false
                            },
                            onCancel = { showTargetSelectorForFrais = false }
                        )
                    }
                }
                
                BreakdownItem("Transferts / Autres", state.breakdown.find { it.type == com.indiza.cotisation.bank.TransactionType.TRANSFERT_SORTIE || it.type == com.indiza.cotisation.bank.TransactionType.TRANSFERT_ENTREE }?.total ?: 0L)

                HorizontalDivider(color = NeonCyan.copy(alpha = 0.3f), thickness = 2.dp)
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SOLDE TOTAL", color = AfroGold, fontWeight = FontWeight.Black)
                    Text("${state.soldeGlobal} FCFA", color = AfroGold, fontWeight = FontWeight.Black)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { 
                Text("FERMER", color = NeonCyan) 
            }
        }
    )
}

@Composable
fun TargetCaisseSelector(
    caisses: List<CaisseEntity>,
    onSelected: (Long?) -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp)) {
        Text("Vers quelle caisse ?", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        
        caisses.forEach { caisse ->
            Surface(
                onClick = { onSelected(caisse.id) },
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Text(
                    text = caisse.libelle, 
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
        
        TextButton(onClick = { onSelected(null) }) {
            Text("Sortie simple (Sans caisse cible)", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onCancel) {
            Text("Annuler", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun BreakdownItem(label: String, amount: Long, isNegative: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        Text(
            "${if (isNegative && amount != 0L) "-" else ""}${kotlin.math.abs(amount)} F", 
            color = if (isNegative) Color.Red.copy(alpha = 0.7f) else Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScheduledToursDialog(
    tours: List<com.indiza.cotisation.bank.TourWithBeneficiaires>,
    onDismiss: () -> Unit
) {
    var selectedTourId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(tours) {
        println("DEBUG_TOUR: ScheduledToursDialog - Nombre de tours reçus: ${tours.size}")
        tours.forEach { t ->
            val status = if (t.tour.estTermine) "TERMINÉ" else "PLANIFIÉ"
            val bens = t.beneficiaires.joinToString { b -> 
                "${b.member.member.nomSaisi} (${b.tourBeneficiaire.nombrePartsAttribuees}P)" 
            }
            println("DEBUG_TOUR:   - Tour n°${t.tour.numeroTour} (DB_ID: ${t.tour.id}, CycleID: ${t.tour.cycleId}) ($status) | Bénéficiaires: $bens")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tours Programmés", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF1A1E2A),
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tours) { tourWithBen ->
                    val isCurrent = !tourWithBen.tour.estTermine && 
                                    tours.filter { !it.tour.estTermine }.firstOrNull()?.tour?.id == tourWithBen.tour.id
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedTourId = if (selectedTourId == tourWithBen.tour.id) null else tourWithBen.tour.id 
                            },
                        color = if (isCurrent) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isCurrent) NeonCyan else Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Tour n°${tourWithBen.tour.numeroTour}", 
                                        color = if (isCurrent) NeonCyan else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrent) {
                                        Text("EN COURS", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                    }
                                }
                                
                                if (tourWithBen.tour.estTermine) {
                                    Icon(Icons.Default.CheckCircle, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                            
                            if (selectedTourId == tourWithBen.tour.id) {
                                Spacer(Modifier.height(12.dp))
                                tourWithBen.beneficiaires.forEach { ben ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).background(AfroGold, CircleShape))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${ben.member.user?.nom ?: ben.member.member.nomSaisi} (${ben.tourBeneficiaire.nombrePartsAttribuees} part${if(ben.tourBeneficiaire.nombrePartsAttribuees > 1) "s" else ""})", 
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { 
                Text("FERMER", color = NeonCyan, fontWeight = FontWeight.Bold) 
            }
        }
    )
}

@Composable
fun SmallShuffleButton(
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onShuffle,
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AfroGold)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "TIRAGE AU SORT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun CaisseActionButtons(
    caisseType: CaisseType?,
    possibilitePret: Boolean,
    peutGererManuellement: Boolean = false,
    onActionSelected: (com.indiza.cotisation.CaisseAction) -> Unit
) {
    Surface(
        color = Color(0xFF1A1E2A),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .width(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "ACTIONS RAPIDES",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Boutons d'actions selon le type de caisse
            when (caisseType) {
                CaisseType.COTISATION_SIMPLE -> {
                    ActionButtonItem(
                        icon = Icons.Default.AddCard,
                        text = "Encaisser Cotisation",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.COTISATION) }
                    )
                    ActionButtonItem(
                        icon = Icons.Default.Payments,
                        text = "Attribuer Cagnotte",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.TOUR_ENCAISSER) }
                    )
                    ActionButtonItem(
                        icon = Icons.Default.Visibility,
                        text = "Voir Ordre Tours",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.VOIR_TOURS) }
                    )
                    ActionButtonItem(
                        icon = Icons.Default.Notifications,
                        text = "Relancer les retardataires",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.RELANCE) }
                    )
                }
                CaisseType.PRET -> {
                    ActionButtonItem(
                        icon = Icons.Default.AttachMoney,
                        text = "Accorder un Prêt",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.PRET) }
                    )
                    ActionButtonItem(
                        icon = Icons.Default.Payments,
                        text = "Remboursement",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.REMBOURSEMENT) }
                    )
                }
                CaisseType.SECOURS -> {
                    ActionButtonItem(
                        icon = Icons.Default.HealthAndSafety,
                        text = "Sortie de Secours",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.SORTIE_SECOURS) }
                    )
                }
                CaisseType.DEVELOPPEMENT, CaisseType.FONCTIONNEMENT, CaisseType.PERSONNALISEE -> {
                    ActionButtonItem(
                        icon = Icons.Default.AddCard,
                        text = "Encaisser Montant",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.COTISATION) }
                    )
                    ActionButtonItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        text = "Effectuer une Sortie",
                        onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.SORTIE) }
                    )
                }
                null -> {}
            }

            // Optionnel : Ajout du prêt si autorisé pour cette caisse (cas mixte)
            if (possibilitePret && caisseType != CaisseType.PRET) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                ActionButtonItem(
                    icon = Icons.Default.AttachMoney,
                    text = "Prêt Exceptionnel",
                    onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.PRET) }
                )
            }

            if (peutGererManuellement) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                ActionButtonItem(
                    icon = Icons.Default.Add,
                    text = "Entrée Manuelle",
                    onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.TRANSFERT_ENTREE) }
                )
                ActionButtonItem(
                    icon = Icons.Default.Close,
                    text = "Sortie Manuelle",
                    onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.SORTIE) }
                )
            }
        }
    }
}

@Composable
fun ActionButtonItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = AfroGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun RowScope.AdminButton(
    text: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
fun ManualMovementDialog(
    type: com.indiza.cotisation.bank.TransactionType,
    montant: String,
    onMontantChange: (String) -> Unit,
    motif: String,
    onMotifChange: (String) -> Unit,
    isRedistribution: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = if (type == com.indiza.cotisation.bank.TransactionType.TRANSFERT_ENTREE) "Entrée Manuelle" else "Sortie Manuelle"
    val accentColor = if (type == com.indiza.cotisation.bank.TransactionType.TRANSFERT_ENTREE) NeonCyan else Color.Red

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = accentColor, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = montant,
                    onValueChange = onMontantChange,
                    label = { Text("Montant (FCFA)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = accentColor,
                        focusedBorderColor = accentColor
                    )
                )

                if (isRedistribution && type == com.indiza.cotisation.bank.TransactionType.SORTIE) {
                    Text(
                        "Note : Les sorties sur caisse à redistribution sont limitées aux amendes et frais.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AfroGold
                    )
                }

                OutlinedTextField(
                    value = motif,
                    onValueChange = onMotifChange,
                    label = { Text("Motif", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = accentColor,
                        focusedBorderColor = accentColor
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler", color = Color.Gray)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        enabled = montant.isNotEmpty() && motif.isNotEmpty()
                    ) {
                        Text("Confirmer", color = if (accentColor == NeonCyan) Color.Black else Color.White)
                    }
                }
            }
        }
    }
}
