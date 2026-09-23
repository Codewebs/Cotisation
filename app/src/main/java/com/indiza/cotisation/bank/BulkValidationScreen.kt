package com.indiza.cotisation.bank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.CaisseAction
import com.indiza.cotisation.caisses.CaisseEntity
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.CosmicBackground
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun BulkValidationScreen(
    caisse: CaisseEntity,
    actionType: CaisseAction,
    participants: List<ParticipantWithParts>,
    cycleId: Long,
    viewModel: TransactionViewModel,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val soldeActuel by viewModel.soldeActuel.collectAsStateWithLifecycle()
    val selected = remember(participants) { mutableStateListOf<Boolean>().apply { addAll(participants.map { true }) } }
    val selectedParticipants = participants.filterIndexed { index, _ -> selected.getOrNull(index) != false }
    
    // Pour TOUR_ENCAISSER, on doit calculer la somme des parts de cagnotte théoriques
    // Si TransactionViewModel.validerEnMasse utilise montantUnitaire (cagnotte d'un membre)
    val totalADeduire = if (actionType == CaisseAction.TOUR_ENCAISSER) {
        selectedParticipants.size * (caisse.montantFixe ?: 0L)
    } else {
        selectedParticipants.sumOf { (caisse.montantFixe ?: 0L) * it.nombreParts }
    }

    val soldeInsuffisant = actionType == CaisseAction.TOUR_ENCAISSER && totalADeduire > soldeActuel

    CosmicBackground {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Récapitulatif global", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if(actionType == CaisseAction.COTISATION) "Encaissement des cotisations" else "Remise des cagnottes",
                        color = NeonCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Cycle n°$cycleId",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                Text(
                    text = caisse.libelle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium
                )

                if (soldeInsuffisant) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Text(
                            text = "Solde insuffisant ($soldeActuel FCFA) pour distribuer $totalADeduire FCFA",
                            color = Color.Red,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(participants) { index, p ->
                        val montant = if (actionType == CaisseAction.COTISATION)
                            (caisse.montantFixe ?: 0L) * p.nombreParts
                        else (caisse.montantFixe ?: 0L)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF101827).copy(alpha = 0.82f)),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected.getOrNull(index) != false,
                                    onCheckedChange = { selected[index] = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = NeonCyan,
                                        uncheckedColor = Color.White.copy(alpha = 0.5f)
                                    )
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        p.memberWithUser.member.nomSaisi,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${p.nombreParts} part${if (p.nombreParts > 1) "s" else ""}",
                                        color = AfroGold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                Text(
                                    text = "$montant FCFA",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (actionType == CaisseAction.TOUR_ENCAISSER) "TOTAL À DISTRIBUER" else "TOTAL À ENCAISSER",
                            fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelLarge
                        )
                        Text("${totalADeduire} FCFA", color = NeonCyan, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    }
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        viewModel.validerEnMasse(caisse, actionType, selectedParticipants, cycleId) { result ->
                            if (result.isSuccess) {
                                onConfirm()
                            } else {
                                android.widget.Toast.makeText(context, result.exceptionOrNull()?.message ?: "Erreur", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = totalADeduire > 0 && !soldeInsuffisant,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, disabledContainerColor = Color.Gray.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (soldeInsuffisant) "SOLDE INSUFFISANT" else "CONFIRMER L'ENREGISTREMENT",
                        color = if (totalADeduire > 0 && !soldeInsuffisant) Color.Black else Color.White, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}