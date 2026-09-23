package com.indiza.cotisation.bank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan
import com.indiza.cotisation.tools.TextColor1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CagnotteValidationScreen(
    caisse: CaisseEntity,
    participant: ParticipantWithParts,
    onDismiss: () -> Unit,
    viewModel: TransactionViewModel,
    cycleId: Long
) {


    var inclureDettes by remember { mutableStateOf(false) }
    var inclureEchecs by remember { mutableStateOf(false) }
    var inclurePenalites by remember { mutableStateOf(false) }

    var details by remember { mutableStateOf(CagnotteDetails()) }
    var montantFinal by remember { mutableStateOf("0") }


    var cycleDejaCloture by remember { mutableStateOf(false) }

    var nbPartsAEncaisser by remember { mutableIntStateOf(1) }
    var partsPlanifiees by remember { mutableIntStateOf(0) }

    val fraisParPart = caisse.fraisCaisse ?: 0L
    val totalFrais = fraisParPart * nbPartsAEncaisser

    var showNotifDialog by remember { mutableStateOf(false) }
    val tontineMembers by viewModel.getTontineMembers(caisse.tontineId).collectAsState(initial = emptyList<TontineMemberWithUser>())
    var isTontineOnline by remember { mutableStateOf(false) }

    LaunchedEffect(caisse.id, participant.member.id, nbPartsAEncaisser) {
        details = viewModel.getCagnotteDetails(caisse, participant, cycleId, nbPartsAEncaisser)
    }

    LaunchedEffect(inclureEchecs, details, nbPartsAEncaisser) {
        val brut = details.brut
        val echecs = if (inclureEchecs) details.echecs else 0L
        val calculCaisse = brut - echecs
        montantFinal = if (calculCaisse < 0) "0" else calculCaisse.toString()
    }

    var peutValider by remember { mutableStateOf(false) }

    var soldeCaisseReel by remember { mutableStateOf(0L) }
    var sommeCotisationsCycle by remember { mutableStateOf(0L) }
    var peutValiderStatut by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(caisse.id, cycleId) {
        peutValider = viewModel.estPretPourCagnotte(caisse.id, cycleId)
    }

    LaunchedEffect(caisse.id, cycleId) {
        val (cotis, solde) = viewModel.getInfoFinanciereCagnotte(caisse.id, cycleId)
        sommeCotisationsCycle = cotis
        soldeCaisseReel = solde
        peutValiderStatut = viewModel.estPretPourCagnotte(caisse.id, cycleId)

        val cycle = viewModel.cycleDao.getCycleById(cycleId)
        cycleDejaCloture = cycle?.estCloture ?: false

        val tontine = viewModel.caisseDao.getTontineByCaisse(caisse.id)
        isTontineOnline = tontine?.isOnline ?: false
    }

    val montantSaisi = montantFinal.toLongOrNull() ?: 0L
    val depasseSolde = montantSaisi > soldeCaisseReel

    val maxPartsDisponibles = participant.nombreParts

    LaunchedEffect(caisse.id, cycleId, participant.member.id) {
        val planned = viewModel.getPartsPlanifiees(participant.member.id, cycleId)
        if (planned != null) {
            partsPlanifiees = planned
            nbPartsAEncaisser = planned
        }
    }

    CosmicBackground {
        Scaffold(
            containerColor = Color.White, // Fond blanc pour lisibilité
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Remise de Cagnotte", color = Color.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bouton de Confirmation
                        Button(
                            onClick = {
                                viewModel.enregistrerCagnotte(
                                    caisseId = caisse.id,
                                    memberId = participant.member.id,
                                    montantCaisse = montantFinal.toLongOrNull() ?: 0L,
                                    nbParts = nbPartsAEncaisser,
                                    montantDettes = if (inclureDettes) details.dettes else 0L,
                                    montantPenalites = if (inclurePenalites) details.penalites else 0L,
                                    montantEchecs = if (inclureEchecs) details.echecs else 0L,
                                    isOnline = isTontineOnline,
                                    onSuccess = {
                                        showNotifDialog = true
                                    },
                                    cycleId = cycleId
                                )
                            },
                            enabled = peutValiderStatut && !depasseSolde && montantSaisi > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (peutValider) AfroGold else Color.Gray,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                if (peutValider) "VALIDER LA REMISE" else "EN ATTENTE DES COTISATIONS",
                                color = if (peutValider) Color.Black else Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Annuler", color = Color.Gray)
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OptionSwitch(
                    label = "Déduire les échecs (Cotisations impayées)",
                    checked = inclureEchecs,
                    labelColor = Color.Black,
                    onCheckedChange = { inclureEchecs = it }
                )

                OptionSwitch(
                    label = "Déduire les dettes de prêt",
                    checked = inclureDettes,
                    labelColor = Color.Black,
                    onCheckedChange = { inclureDettes = it }
                )

                OptionSwitch(
                    label = "Déduire les pénalités de retard",
                    labelColor = Color.Black,
                    checked = inclurePenalites,
                    onCheckedChange = { inclurePenalites = it }
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(AfroGold.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, AfroGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AfroGold, modifier = Modifier.size(40.dp))
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = participant.member.nomSaisi,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Bénéficiaire du tour",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )

                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nombre de parts à liquider :", color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (nbPartsAEncaisser > 1) nbPartsAEncaisser-- },
                            enabled = partsPlanifiees == 0 // Désactivé si planifié
                        ) {
                            Icon(Icons.Default.Remove, null, tint = if (partsPlanifiees == 0) NeonCyan else Color.Gray)
                        }
                        Text("$nbPartsAEncaisser", color = Color.Black, style = MaterialTheme.typography.titleLarge)
                        IconButton(
                            onClick = { if (nbPartsAEncaisser < maxPartsDisponibles) nbPartsAEncaisser++ },
                            enabled = partsPlanifiees == 0 // Désactivé si planifié
                        ) {
                            Icon(Icons.Default.Add, null, tint = if (partsPlanifiees == 0) NeonCyan else Color.Gray)
                        }
                    }
                }

                if (partsPlanifiees > 0) {
                    Text(
                        "Tour programmé pour $partsPlanifiees part(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (totalFrais > 0) {
                    Text(
                        "Frais de caisse : $totalFrais FCFA ($fraisParPart F / part)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (depasseSolde) Color.Red.copy(alpha = 0.1f)
                        else NeonCyan.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(1.dp, if (depasseSolde) Color.Red else NeonCyan.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SORTIE DE CAISSE (BRUT - ÉCHECS)", style = MaterialTheme.typography.labelMedium, color = NeonCyan, fontWeight = FontWeight.Bold)

                        BasicTextField(
                            value = montantFinal,
                            onValueChange = { montantFinal = it },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                color = if (depasseSolde) Color.Red else NeonCyan,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        val currentMontantCaisse = montantFinal.toLongOrNull() ?: 0L
                        val currentDettes = if (inclureDettes) details.dettes else 0L
                        val currentPenalites = if (inclurePenalites) details.penalites else 0L
                        val currentFrais = totalFrais
                        val net = currentMontantCaisse - currentDettes - currentPenalites - currentFrais

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (inclureEchecs && details.echecs > 0) {
                                Text("- Échecs : ${details.echecs} F", color = Color.Red.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                            }
                            if (currentDettes > 0) {
                                Text("- Dettes : $currentDettes F", color = Color.Red.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                            }
                            if (currentPenalites > 0) {
                                Text("- Pénalités : $currentPenalites F", color = Color.Red.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                            }
                            if (currentFrais > 0) {
                                Text("- Frais caisse : $currentFrais F", color = Color.Red.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "NET À PERCEVOIR : ${if (net < 0) 0 else net} FCFA",
                            color = AfroGold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Maximum disponible en caisse : $soldeCaisseReel FCFA",
                            color = if (depasseSolde) Color.Red else Color.DarkGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                if (!peutValider) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Impossible de valider : certains membres n'ont pas encore cotisé pour ce tour.",
                                color = Color.Black,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Alertes
                if (!peutValiderStatut) {
                    ValidationAlert(text = "En attente des cotisations de tous les membres.")
                } else if (depasseSolde) {
                    ValidationAlert(text = "Fonds insuffisants en caisse (Solde: $soldeCaisseReel).")
                }
            }
        }

        if (showNotifDialog) {
            PostPayoutNotificationDialog(
                members = tontineMembers,
                isOnline = isTontineOnline,
                onDismiss = onDismiss,
                onSend = { _, _, _ ->
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun OptionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, labelColor: Color = Color.Black) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .background(if(labelColor == Color.Black) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = labelColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
data class CagnotteDetails(
    val brut: Long = 0L,
    val dettes: Long = 0L,
    val echecs: Long = 0L,
    val penalites: Long = 0L
)
@Composable
fun ValidationAlert(text: String) {
    Surface(
        color = Color.Red.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.Black, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}