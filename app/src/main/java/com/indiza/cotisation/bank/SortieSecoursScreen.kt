package com.indiza.cotisation.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Output
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.indiza.cotisation.tools.NeonCyan
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.indiza.cotisation.tontine.TontineMemberWithUser
import com.indiza.cotisation.tontinemembers.ParticipantWithParts
import com.indiza.cotisation.tools.AfroGold
import com.indiza.cotisation.tools.GlassWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortieSecoursScreen(
    caisseId: Long,
    participant: ParticipantWithParts,
    onDismiss: () -> Unit,
    cycleId : Long,
    transactionType: TransactionType = TransactionType.SORTIE_SECOURS,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    var montant by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // État pour le DropdownMenu
    var expanded by remember { mutableStateOf(false) }
    var motifSelectionne by remember { mutableStateOf(MotifSortie.AIDE_SOCIALE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (transactionType == TransactionType.SORTIE) "Nouvelle Dépense / Sortie" else "Nouvelle Sortie : ${participant.memberWithUser.member.nomSaisi}",
            style = MaterialTheme.typography.headlineSmall,
            color = AfroGold
        )

        Spacer(Modifier.height(16.dp))

        // --- SÉLECTEUR DE MOTIF (Dropdown) ---
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = motifSelectionne.name.replace("_", " "),
                onValueChange = {},
                readOnly = true,
                label = { Text("Motif de la sortie") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassWhite
                ),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1A1A1A))
            ) {
                MotifSortie.values().forEach { motif ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                motif.name.replace("_", " "),
                                color = Color.White
                            )
                        },
                        onClick = {
                            motifSelectionne = motif
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- CHAMP MONTANT ---
        OutlinedTextField(
            value = montant,
            onValueChange = { if (it.all { char -> char.isDigit() }) montant = it },
            label = { Text("Montant à décaisser (FCFA)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = GlassWhite,
                focusedLabelColor = NeonCyan,
                unfocusedLabelColor = GlassWhite,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.height(16.dp))

        // --- CHAMP DESCRIPTION ---
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Détails / Justification") },
            placeholder = { Text("Ex: Décès du parent de l'adhérent...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = GlassWhite,
                focusedLabelColor = NeonCyan,
                unfocusedLabelColor = GlassWhite,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.height(24.dp))

        // --- BOUTON DE VALIDATION ---
        Button(
            onClick = {
                if (montant.isNotEmpty()) {
                    viewModel.enregistrerSortie(
                        caisseId = caisseId,
                        memberId = participant.memberWithUser.member.id,
                        montant = montant.toLong(),
                        motif = motifSelectionne,
                        description = description,
                        cycleId = cycleId,
                        type = transactionType
                    )
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red), // Rouge car c'est une sortie d'argent
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Output, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("VALIDER LE DÉCAISSEMENT", fontWeight = FontWeight.Bold)
        }
    }
}