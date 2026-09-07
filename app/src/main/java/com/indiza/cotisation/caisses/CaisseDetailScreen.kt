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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.tools.NeonCyan
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.indiza.cotisation.Screen
import com.indiza.cotisation.bank.TransactionViewModel
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.AfroSunsetGradient
import com.indiza.cotisation.tools.DeepWhite
import com.indiza.cotisation.tools.GlassWhite

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
    var showAddDialog by remember { mutableStateOf(false) }
    var showConfirmShuffle by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    var cycleMenuExpanded by remember { mutableStateOf(false) }
    var showToursDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cycleId) {
        viewModel.selectCycleById(cycleId)
    }

    val cycle by viewModel.getCycle(cycleId).collectAsState(initial = null)

    val nouveauCycleId by transactionViewModel.nouveauCycleId.collectAsState()


    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (!state.isReadOnly) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showActionMenu) {
                        CaisseActionButtons(
                            caisseType = state.caisse?.type,
                            possibilitePret = state.caisse?.possibilitePret ?: false,
                            onActionSelected = { action ->
                                showActionMenu = false
                                if (action == com.indiza.cotisation.CaisseAction.VOIR_TOURS) {
                                    showToursDialog = true
                                } else {
                                    navController.navigate("selection_membre/$caisseId/${action.name}/$cycleId")
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
                        containerColor = NeonCyan,
                        contentColor = Color.Black
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(
                color = Color(0xFF111827).copy(alpha = 0.85f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.15f)),
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
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.ReportsMenu.createRoute(caisseId, cycleId))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Rapports",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                navController.navigate(Screen.TransactionJournal.createRoute(caisseId, cycleId))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Journal",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showEditCaisse = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Settings, "Paramètres", tint = NeonCyan)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { cycleMenuExpanded = true },
                color = Color(0xFF1A1E2A),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.18f))
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

                    if (!state.isReadOnly || state.peutCloturerTour) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cycle?.let { cycleEntity ->
                                if (state.peutCloturerCycle) {
                                    AdminButton(
                                        text = "Clôturer Cycle",
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
                            }

                            if (state.peutAjouterTour) {
                                AdminButton(
                                    text = "Ajouter Tour",
                                    color = AfroGold,
                                    icon = Icons.Default.Add,
                                    onClick = {
                                        navController.navigate("creation_tour/${caisseId}/${cycleId}")
                                    }
                                )
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
                    CaisseHeader(
                        caisse = state.caisse,
                        currentCycle = state.currentCycle,
                        soldeDuCycle = state.soldeDuCycle + state.soldePrecedent,
                        soldePrecedent = state.soldePrecedent,
                        totalPrets = state.totalPrets,
                        toursCompletes = state.toursCompletes,
                        nombreCotisants = state.dejaCotise.size,
                        nombreBeneficiaires = state.dejaBeneficiaires.size,
                        prochainBeneficiaire = state.beneficiaireActuel?.memberWithUser?.user?.nom,
                        totalParticipants = state.participants.size,
                        montantCible = (state.caisse?.montantFixe ?: 0L) * maxOf(state.participants.size, 1),
                        dateLimitePaiement = state.caisse?.dateLimiteJour,
                        totalAmendes = state.totalAmendes,
                        totalPenalites = state.totalPenalites,
                        montantDistribue = state.montantDistribue,
                        beneficiaires = state.beneficiairesDuCycle.map { it.member.member.nomSaisi }.distinct(),
                        montantRecolte = state.montantRecolte,
                        onReportClick = {
                            val cycleNum = state.currentCycle?.numeroCycle ?: 1
                            navController.navigate("cycle_report/$caisseId/$cycleId/$cycleNum")
                        },
                        onToggleLock = {
                            viewModel.toggleOrdreFige()
                        }
                    )
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
                                color = NeonCyan,
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
                                    onMoveUp = { if (state.caisse?.ordreFige != true) viewModel.moveParticipant(index, index - 1) },
                                    onMoveDown = { if (state.caisse?.ordreFige != true) viewModel.moveParticipant(index, index + 1) }
                                )
                            }

                            if (estPlanifie) {
                                IconButton(
                                    onClick = {
                                        transactionViewModel.annulerPositionnementCagnotte(caisseId, memberId, cycleId)
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
                onToggle = { memberId, isSelected, nbParts ->
                    viewModel.toggleParticipant(memberId, isSelected, nbParts)
                },
                onDismiss = { showAddDialog = false }
            )
        }

        if (showToursDialog) {
            ScheduledToursDialog(
                tours = state.toursDetailCycle,
                onDismiss = { showToursDialog = false }
            )
        }
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
            println("DEBUG_TOUR:   - Tour n°${t.tour.numeroTour} ($status) | Bénéficiaires: $bens")
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
                androidx.compose.material3.HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                ActionButtonItem(
                    icon = Icons.Default.AttachMoney,
                    text = "Prêt Exceptionnel",
                    onClick = { onActionSelected(com.indiza.cotisation.CaisseAction.PRET) }
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