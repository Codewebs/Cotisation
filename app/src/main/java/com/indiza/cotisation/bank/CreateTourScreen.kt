package com.indiza.cotisation.bank

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.ui.theme.CotisationDesign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTourScreen(
    caisseId: Long,
    cycleId: Long,
    onTourValidated: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    // 1. Charger les données au démarrage
    LaunchedEffect(caisseId, cycleId) {
        println("DEBUG_TOUR: CreateTourScreen - Lancement loadCreateTourData(caisseId=$caisseId, cycleId=$cycleId)")
        viewModel.loadCreateTourData(caisseId, cycleId)
    }

    // 2. Observer l'état du ViewModel
    val state by viewModel.createTourState.collectAsStateWithLifecycle()

    // Liste des membres sélectionnés pour ce tour
    var selectedBeneficiaires by remember { mutableStateOf(setOf<Long>()) }
    // Map pour stocker le nombre de parts choisi par membre sélectionné
    val partsMap = remember { mutableStateMapOf<Long, Int>() }
    val caisse = state.caisse ?: return
    val participants = state.participants

    // Calcul automatique du prochain bénéficiaire si l'ordre est figé
    LaunchedEffect(participants, state.ordreFige, state.partsEncaisseesMap) {
        if (state.ordreFige && selectedBeneficiaires.isEmpty()) {
            val next = participants.firstOrNull { p ->
                val dejaRecu = state.partsEncaisseesMap[p.member.id] ?: 0
                dejaRecu < p.nombreParts
            }
            next?.let { 
                selectedBeneficiaires = setOf(it.member.id)
                partsMap[it.member.id] = 1
            }
        }
    }

    // Logique de calcul du maximum possible selon le solde
    val potUnitaire = (caisse.montantFixe ?: 0L) * state.totalPartsCaisse
    val maxPotsPossibles = if (potUnitaire > 0) (state.soldeGlobal / potUnitaire).toInt() else 0
    val totalPartsSelectionnees = partsMap.values.sum()
    val peutAjouterDutravail = totalPartsSelectionnees < maxPotsPossibles

    CosmicBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("NOUVEAU TOUR DE CAGNOTTE", fontWeight = FontWeight.Black) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                // Infos de base : Cycle et Caisse
                state.cycle?.let { cycle ->
                    Text(
                        text = "Cycle n°${cycle.numeroCycle} - ${state.caisse?.libelle ?: ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                Text(
                    "Sélectionnez les bénéficiaires et leurs parts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (participants.isEmpty()) {
                        item {
                            Text(
                                "Aucun participant éligible (toutes les parts ont été planifiées ou encaissées).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 32.dp)
                            )
                        }
                    }
                    
                    items(participants) { participant ->
                        val memberId = participant.member.id
                        val isSelected = selectedBeneficiaires.contains(memberId)
                        val partsDejaEncaissees = state.partsEncaisseesMap[memberId] ?: 0
                        val aToutRecu = partsDejaEncaissees >= participant.nombreParts
                        
                        val peutSelectionnerEncore = (peutAjouterDutravail || isSelected) && !aToutRecu

                        BeneficiaireSelectionRow(
                            participant = participant,
                            isSelected = isSelected,
                            currentParts = partsMap[memberId] ?: 1,
                            partsEncaissees = partsDejaEncaissees,
                            enabled = peutSelectionnerEncore && (!state.ordreFige || isSelected),
                            onToggle = { checked ->
                                if (checked) {
                                    if (peutAjouterDutravail) {
                                        selectedBeneficiaires = selectedBeneficiaires + memberId
                                        partsMap[memberId] = 1
                                    }
                                } else {
                                    selectedBeneficiaires = selectedBeneficiaires - memberId
                                    partsMap.remove(memberId)
                                }
                            },
                            onPartsChange = { partsMap[memberId] = it }
                        )
                    }
                }

                // Résumé et Validation
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "PLANIFICATION DES PARTS : $totalPartsSelectionnees / $maxPotsPossibles",
                            color = if (totalPartsSelectionnees >= maxPotsPossibles && maxPotsPossibles > 0) MaterialTheme.colorScheme.primary else if (maxPotsPossibles == 0) Color.Red else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Solde en caisse : ${state.soldeGlobal} FCFA",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (maxPotsPossibles == 0) {
                            Text(
                                text = "Solde insuffisant pour une cagnotte pleine (${potUnitaire} FCFA)",
                                color = Color.Red,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }


                CotisationDesign.PrimaryButton(
                    onClick = {
                        val selections = participants
                            .filter { selectedBeneficiaires.contains(it.member.id) }
                            .map { it to (partsMap[it.member.id] ?: 1) }

                        println("DEBUG_TOUR: CreateTourScreen - Bouton Valider cliqué. Nb bénéficiaires sélectionnés: ${selections.size}")
                        selections.forEach { (p, parts) ->
                            println("DEBUG_TOUR:   - Sélectionné: ${p.member.nomSaisi} pour $parts part(s)")
                        }

                        viewModel.validerTourComplet(caisse.id, cycleId, selections, onTourValidated)
                    },
                    enabled = selectedBeneficiaires.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    text = "Valider le Tour"
                )
            }
        }
    }
}


@Composable
fun BeneficiaireSelectionRow(
    participant: ParticipantWithParts,
    isSelected: Boolean,
    currentParts: Int,
    partsEncaissees: Int = 0,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
    onPartsChange: (Int) -> Unit
) {
    val member = participant.member
    val partsRestantes = participant.nombreParts - partsEncaissees

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected, 
            onCheckedChange = onToggle,
            enabled = enabled || isSelected
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(member.nomSaisi, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            Text(
                text = if (partsEncaissees > 0) "${participant.nombreParts} parts ($partsRestantes restantes)" 
                       else "${participant.nombreParts} parts détenues", 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.Gray
            )
        }

        if (isSelected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (currentParts > 1) onPartsChange(currentParts - 1) }) {
                    Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
                }
                Text("$currentParts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                IconButton(onClick = { if (currentParts < partsRestantes) onPartsChange(currentParts + 1) }) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
