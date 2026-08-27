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
                Text(
                    "Sélectionnez les bénéficiaires et leurs parts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(participants) { participant ->
                        val memberId = participant.memberWithUser.member.id
                        val isSelected = selectedBeneficiaires.contains(memberId)

                        BeneficiaireSelectionRow(
                            participant = participant,
                            isSelected = isSelected,
                            currentParts = partsMap[memberId] ?: 1,
                            onToggle = { checked ->
                                if (checked) {
                                    selectedBeneficiaires = selectedBeneficiaires + memberId
                                    partsMap[memberId] = 1
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
                val totalPartsTour = partsMap.values.sum()
                val montantTotal = totalPartsTour * (caisse.montantFixe ?: 0L)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "TOTAL À DISTRIBUER : $montantTotal FCFA",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Total parts sélectionnées : $totalPartsTour",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                CotisationDesign.PrimaryButton(
                    onClick = {
                        val selections = participants
                            .filter { selectedBeneficiaires.contains(it.memberWithUser.member.id) }
                            .map { it to (partsMap[it.memberWithUser.member.id] ?: 1) }

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
    onToggle: (Boolean) -> Unit,
    onPartsChange: (Int) -> Unit
) {
    val member = participant.memberWithUser.member

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = onToggle)

        Column(modifier = Modifier.weight(1f)) {
            Text(member.nomSaisi, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            Text("${participant.nombreParts} parts détenues", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }

        if (isSelected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (currentParts > 1) onPartsChange(currentParts - 1) }) {
                    Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
                }
                Text("$currentParts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                IconButton(onClick = { if (currentParts < participant.nombreParts) onPartsChange(currentParts + 1) }) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
