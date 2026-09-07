package com.indiza.cotisation.bank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.caisses.TypeInteret
import com.indiza.cotisation.tontine.TontineDetailUiState
import com.indiza.cotisation.tontine.TontineDetailViewModel
import com.indiza.cotisation.tontinemembers.MemberUiModel
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanManagementScreen(
    tontineId: Long,
    onBack: () -> Unit,
    navController: NavController,
    viewModel: LoanManagementViewModel = hiltViewModel(),
    tontineViewModel: TontineDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(tontineId) {
        viewModel.loadData(tontineId)
        tontineViewModel.loadTontine(tontineId)
    }

    val tontineState by tontineViewModel.state.collectAsState()
    val lendableCaisses by viewModel.lendableCaisses.collectAsState()
    val basicFund by viewModel.basicLoanFund.collectAsState()

    var showSetupDialog by remember { mutableStateOf(false) }
    var showLoanForm by remember { mutableStateOf(false) }
    var loanReceiptData by remember { mutableStateOf<PretEntity?>(null) }

    CosmicBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gestion des Prêts", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            val uiState = tontineState
            if (uiState is TontineDetailUiState.Success) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        com.indiza.cotisation.tontine.TontineFinancialSummaryBlock(uiState.financialSummary)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("CAISSE DE PRÊT BASIQUE", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                                Spacer(Modifier.height(8.dp))
                                if (basicFund == null) {
                                    Text("Non configurée. Cette caisse permet d'accorder des prêts à tous et aux externes.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    if (uiState.canInitiateLoans) {
                                        Button(
                                            onClick = { showSetupDialog = true },
                                            modifier = Modifier.padding(top = 12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                        ) {
                                            Text("Activer & Configurer Plafond", color = Color.Black)
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Plafond Actuel : ${basicFund!!.plafondPret} F", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Generic - Ouvert aux externes", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (uiState.canInitiateLoans) {
                                            IconButton(onClick = { showSetupDialog = true }) {
                                                Icon(Icons.Default.Edit, null, tint = NeonCyan)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.hasRequiredRolesForLoans && uiState.canInitiateLoans) {
                        item {
                            Button(
                                onClick = { showLoanForm = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("ACCORDER UN NOUVEAU PRÊT", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (!uiState.hasRequiredRolesForLoans) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, tint = Color.Red)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Il faut au moins 3 rôles officiels (Président, Trésorier, CAC, Secrétaire) pour engager des prêts.", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (showSetupDialog) {
                    SetupBasicFundDialog(
                        lendableCaisses = lendableCaisses,
                        onDismiss = { showSetupDialog = false },
                        onConfirm = { amount, sourceId ->
                            viewModel.setupBasicFund(tontineId, amount, sourceId)
                            showSetupDialog = false
                        }
                    )
                }
                
                if (showLoanForm) {
                    LoanIssuanceDialog(
                        members = uiState.displayMembers,
                        availableFunds = (lendableCaisses + (basicFund?.let { listOf(it) } ?: emptyList())),
                        onDismiss = { showLoanForm = false },
                        onConfirm = { caisseId, mId, amount, taux, typeInt, nomExt, cniExt, guarId ->
                            viewModel.grantLoan(caisseId, mId, amount, 0L, taux, typeInt, nomExt, cniExt, guarId) {
                                showLoanForm = false
                                // Simuler la réception de l'objet créé
                                loanReceiptData = PretEntity(
                                    caisseId = caisseId,
                                    memberId = mId,
                                    montantPrincipal = amount,
                                    montantInterets = (amount * taux / 100).toLong(),
                                    tauxInteretApplique = taux,
                                    typeInteret = typeInt,
                                    dateEcheance = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
                                    nomEmprunteurExterne = nomExt,
                                    cniEmprunteurExterne = cniExt,
                                    guarantorMemberId = guarId
                                )
                            }
                        }
                    )
                }

                loanReceiptData?.let { data ->
                    LoanReceiptDialog(
                        data = data,
                        members = uiState.displayMembers,
                        fundName = (lendableCaisses + (basicFund?.let { listOf(it) } ?: emptyList())).find { it.id == data.caisseId }?.libelle ?: "",
                        onDismiss = { loanReceiptData = null }
                    )
                }
            }
        }
    }
}

@Composable
fun SetupBasicFundDialog(
    lendableCaisses: List<CaisseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedCaisseId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuration Plafond Prêt") },
        text = {
            Column {
                Text("L'argent du plafond sera déduit d'une caisse existante pour garantir la traçabilité.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Montant du plafond (F)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(16.dp))
                
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(lendableCaisses.find { it.id == selectedCaisseId }?.libelle ?: "Choisir Caisse Source")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        lendableCaisses.forEach { caisse ->
                            DropdownMenuItem(
                                text = { Text(caisse.libelle) },
                                onClick = {
                                    selectedCaisseId = caisse.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val a = amount.toLongOrNull() ?: 0L
                    if (a > 0 && selectedCaisseId != null) {
                        onConfirm(a, selectedCaisseId!!)
                    }
                },
                enabled = amount.isNotEmpty() && selectedCaisseId != null
            ) { Text("VALIDER") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULER") } }
    )
}

@Composable
fun LoanIssuanceDialog(
    members: List<MemberUiModel>,
    availableFunds: List<CaisseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long?, Long, Double, TypeInteret, String?, String?, Long?) -> Unit
) {
    var isExternal by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var selectedCaisseId by remember { mutableStateOf<Long?>(null) }
    var nomExterne by remember { mutableStateOf("") }
    var cniExterne by remember { mutableStateOf("") }
    var guarantorId by remember { mutableStateOf<Long?>(null) }
    
    var fundExpanded by remember { mutableStateOf(false) }
    var memberExpanded by remember { mutableStateOf(false) }
    var guarantorExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accorder un Prêt") },
        text = {
            LazyColumn {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isExternal, onCheckedChange = { isExternal = it })
                        Text("Emprunteur Externe")
                    }
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    Box {
                        OutlinedButton(onClick = { fundExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(availableFunds.find { it.id == selectedCaisseId }?.libelle ?: "Choisir Caisse")
                        }
                        DropdownMenu(expanded = fundExpanded, onDismissRequest = { fundExpanded = false }) {
                            availableFunds.forEach { caisse ->
                                DropdownMenuItem(text = { Text(caisse.libelle) }, onClick = { selectedCaisseId = caisse.id; fundExpanded = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (isExternal) {
                    item {
                        OutlinedTextField(value = nomExterne, onValueChange = { nomExterne = it }, label = { Text("Nom complet") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = cniExterne, onValueChange = { cniExterne = it }, label = { Text("N° CNI / Passeport") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))
                        Text("Membre Avaliseur :", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(onClick = { guarantorExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(members.find { it.id == guarantorId?.toString() }?.displayName ?: "Choisir Avaliseur")
                            }
                            DropdownMenu(expanded = guarantorExpanded, onDismissRequest = { guarantorExpanded = false }) {
                                members.forEach { m ->
                                    DropdownMenuItem(text = { Text(m.displayName) }, onClick = { guarantorId = m.id.toLongOrNull(); guarantorExpanded = false })
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Box {
                            OutlinedButton(onClick = { memberExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(members.find { it.id == selectedMemberId?.toString() }?.displayName ?: "Choisir Membre")
                            }
                            DropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false }) {
                                members.forEach { m ->
                                    DropdownMenuItem(text = { Text(m.displayName) }, onClick = { selectedMemberId = m.id.toLongOrNull(); memberExpanded = false })
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Montant du prêt (F)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val a = amount.toLongOrNull() ?: 0L
                    if (selectedCaisseId != null && a > 0) {
                        onConfirm(selectedCaisseId!!, selectedMemberId, a, 10.0, TypeInteret.SIMPLE, nomExterne.takeIf { isExternal }, cniExterne.takeIf { isExternal }, guarantorId)
                    }
                },
                enabled = amount.isNotEmpty() && selectedCaisseId != null && (if (isExternal) nomExterne.isNotEmpty() && guarantorId != null else selectedMemberId != null)
            ) { Text("CONFIRMER") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULER") } }
    )
}

@Composable
fun LoanReceiptDialog(
    data: PretEntity,
    members: List<MemberUiModel>,
    fundName: String,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fiche de Prêt Générée", color = NeonCyan) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("DÉTAILS DE L'OPÉRATION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bénéficiaire :", color = Color.Gray)
                    Text(data.nomEmprunteurExterne ?: members.find { it.id == data.memberId?.toString() }?.displayName ?: "N/A", fontWeight = FontWeight.Bold)
                }
                if (data.cniEmprunteurExterne != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CNI/Pass :", color = Color.Gray)
                        Text(data.cniEmprunteurExterne, fontWeight = FontWeight.Bold)
                    }
                }
                if (data.guarantorMemberId != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Avalisé par :", color = Color.Gray)
                        Text(members.find { it.id == data.guarantorMemberId.toString() }?.displayName ?: "ID ${data.guarantorMemberId}", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Montant Principal :", color = Color.Gray)
                    Text("${data.montantPrincipal} F", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Intérêts prévus :", color = Color.Gray)
                    Text("${data.montantInterets} F", color = Color(0xFF50FA7B))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taux appliqué :", color = Color.Gray)
                    Text("${data.tauxInteretApplique}%", fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Échéance :", color = Color.Gray)
                    Text(sdf.format(Date(data.dateEcheance)), fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Caisse Source :", color = Color.Gray)
                    Text(fundName, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                Text("FERMER", color = Color.Black)
            }
        }
    )
}
