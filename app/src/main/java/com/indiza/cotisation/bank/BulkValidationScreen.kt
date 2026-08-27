package com.indiza.cotisation.bank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val selected = remember(participants) { mutableStateListOf<Boolean>().apply { addAll(participants.map { true }) } }
    val selectedParticipants = participants.filterIndexed { index, _ -> selected.getOrNull(index) != false }
    val totalMontant = selectedParticipants.sumOf {
        if (actionType == CaisseAction.COTISATION) (caisse.montantFixe ?: 0L) * it.nombreParts
        else (caisse.montantFixe ?: 0L)
    }

    CosmicBackground {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Récapitulatif global", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = if(actionType == CaisseAction.COTISATION) "Encaissement des cotisations" else "Remise des cagnottes",
                    color = NeonCyan,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

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
                        Text("TOTAL À ENCAISSER", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelLarge)
                        Text("$totalMontant FCFA", color = NeonCyan, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Button(
                    onClick = {
                        viewModel.validerEnMasse(caisse, actionType, selectedParticipants, cycleId)
                        onConfirm()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = totalMontant > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, disabledContainerColor = Color.Gray.copy(alpha = 0.3f))
                ) {
                    Text("CONFIRMER L'ENREGISTREMENT", color = if (totalMontant > 0) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}