package com.indiza.cotisation.bank

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indiza.cotisation.caisses.CaisseDetailViewModel
import com.indiza.cotisation.tools.NeonCyan

@Composable
fun RemboursementScreen(
    caisseId: Long,
    memberId: Long,
    memberName: String,
    cycleId: Long,
    cvm: CaisseDetailViewModel = hiltViewModel(),
    viewModel: TransactionViewModel = hiltViewModel(),
    onSuccess: () -> Unit
) {
    val resteAPayer by viewModel.getEtatDetteMembre(caisseId, memberId).collectAsState(initial = 0L)
    var montantSaisi by remember { mutableStateOf("") }
    var estInteret by remember { mutableStateOf(false) }
    var originCaisseId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(memberId, caisseId) {
        originCaisseId = viewModel.getOriginCaisseId(memberId, caisseId)
    }

    // On convertit la dette (négative) en montant positif pour l'affichage
    val detteAffichage = -(resteAPayer ?: 0L)
    val detteMaximale = -(resteAPayer ?: 0L)

    // Validation : le montant doit être un nombre, supérieur à 0 et inférieur ou égal à la dette
    val montantLong = montantSaisi.toLongOrNull() ?: 0L
    val isMontantValide = montantLong > 0 && montantLong <= detteMaximale
    val isTropEleve = montantLong > detteMaximale

    val state by cvm.state.collectAsStateWithLifecycle()
    val cycleId = state.currentCycle?.id ?: return
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text("Remboursement de $memberName", style = MaterialTheme.typography.titleLarge, color = NeonCyan)

        Card(
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Dette actuelle", color = Color.Gray)
                Text("$detteAffichage FCFA", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
        }

        OutlinedTextField(
            value = montantSaisi,
            onValueChange = { montantSaisi = it },
            label = { Text("Montant versé") },
            modifier = Modifier.fillMaxWidth(),
            isError = isTropEleve,
            supportingText = {
                if (isTropEleve) {
                    Text("Le montant dépasse la dette actuelle ($detteMaximale FCFA)", color = Color.Red)
                }
            },
            trailingIcon = {
                if (isTropEleve) Icon(Icons.Default.Error, "Erreur", tint = Color.Red)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                errorBorderColor = Color.Red
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = estInteret, onCheckedChange = { estInteret = it })
            Text("Paiement d'intérêts (ne réduit pas le capital)", color = Color.White)
        }

        Button(
            onClick = {
                if (isMontantValide) {
                    viewModel.enregistrerRemboursement(caisseId, memberId, montantLong, cycleId, estInteret, originCaisseId)
                    onSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = isMontantValide, // Bouton désactivé si erreur
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = if (isTropEleve) "MONTANT TROP ÉLEVÉ" else "VALIDER LE VERSEMENT",
                color = if (isMontantValide) Color.Black else Color.White
            )
        }
    }
}